/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.buildKoltinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.serialization.konan.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File

internal class K2MetadataKlibSerializer {
    fun serialize(environment: KotlinCoreEnvironment) {
        val configuration = environment.configuration

        val dependencyContainer = KlibMetadataDependencyContainer(
            configuration,
            LockBasedStorageManager("K2MetadataKlibSerializer")
        )

        val analyzer =
            runAnalysisForK2Metadata(environment, dependencyContainer)

        if (analyzer?.hasErrors() != false) return

        val (bindingContext, moduleDescriptor) = analyzer.analysisResult

        val destDir = checkNotNull(environment.destDir)
        performSerialization(configuration, bindingContext, moduleDescriptor, destDir)
    }

    private fun performSerialization(
        configuration: CompilerConfiguration,
        bindingContext: BindingContext,
        module: ModuleDescriptor,
        destDir: File
    ) {
        val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION) as? BuiltInsBinaryVersion ?: BuiltInsBinaryVersion.INSTANCE

        val serializedMetadata: SerializedMetadata = KlibMetadataMonolithicSerializer(
            configuration.languageVersionSettings,
            metadataVersion,
            DescriptorTable(),
            bindingContext,
            includeOnlyPackagesFromSource = true
        ).serializeModule(module)

        val versions: KotlinLibraryVersioning = run {
            val abiVersion = KotlinAbiVersion.CURRENT
            val compilerVersion = KotlinCompilerVersion.getVersion()
            KotlinLibraryVersioning(
                abiVersion = abiVersion,
                libraryVersion = null,
                compilerVersion = compilerVersion,
                metadataVersion = null,
                irVersion = null
            )
        }

        buildKoltinLibrary(
            emptyList(),
            serializedMetadata,
            null,
            versions,
            destDir.absolutePath,
            configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            nopack = true,
            manifestProperties = null,
            dataFlowGraph = null
        )
    }
}

private class KlibMetadataDependencyContainer(
    private val configuration: CompilerConfiguration,
    private val storageManager: StorageManager
) : CommonDependenciesContainer {

    private val kotlinLibraries = storageManager.createLazyValue {
        val classpathFiles =
            configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).filterIsInstance<JvmClasspathRoot>().map(JvmContentRoot::file)

        val klibFiles = classpathFiles
            .filter { it.extension == "klib" || it.isDirectory }

        klibFiles.map { createKotlinLibrary(org.jetbrains.kotlin.konan.file.File(it.absolutePath)) }
    }

    private val builtIns
        get() = DefaultBuiltIns.Instance

    private class KlibModuleInfo(
        override val name: Name,
        val kotlinLibrary: KotlinLibrary,
        override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>,
        private val dependOnOldBuiltIns: Boolean,
        private val dependOnKlibModules: () -> Iterable<ModuleInfo>
    ) : ModuleInfo {
        override fun dependencies(): List<ModuleInfo> =
            mutableListOf<ModuleInfo>(this).apply { addAll(dependOnKlibModules()) }

        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
            if (dependOnOldBuiltIns) ModuleInfo.DependencyOnBuiltIns.LAST else ModuleInfo.DependencyOnBuiltIns.NONE

        override val platform: TargetPlatform
            get() = CommonPlatforms.defaultCommonPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = CommonPlatformAnalyzerServices
    }

    override val moduleInfos: Set<ModuleInfo> by lazy {
        moduleDescriptorsForKotlinLibraries.mapTo(mutableSetOf()) { (kotlinLibrary, moduleDescriptor) ->
            KlibModuleInfo(moduleDescriptor.name, kotlinLibrary, emptyMap(), true) { moduleInfos }
        }
    }

    private val moduleDescriptorsForKotlinLibraries: Map<KotlinLibrary, ModuleDescriptorImpl> by lazy {
        val result = kotlinLibraries().keysToMap { library ->
            val moduleHeader = parseModuleHeader(library.moduleHeaderData)
            val moduleName = Name.special(moduleHeader.moduleName)
            val moduleOrigin = DeserializedKlibModuleOrigin(library)
            MetadataFactories.DefaultDescriptorFactory.createDescriptor(
                moduleName, storageManager, builtIns, moduleOrigin
            )
        }
        val resultValues = result.values.toList()
        resultValues.forEach { module ->
            module.setDependencies(resultValues)
        }
        result
    }

    override fun packageFragmentProviderForModuleInfo(
        moduleInfo: ModuleInfo
    ): PackageFragmentProvider? {
        if (moduleInfo !in moduleInfos)
            return null
        moduleInfo as KlibModuleInfo
        return packageFragmentProviderForKotlinLibrary(moduleInfo.kotlinLibrary)
    }

    override fun setModuleDescriptorForPackageFragmentProviders(moduleDescriptor: ModuleDescriptorImpl) {
        check(moduleDescriptorForPackageFragments == null) {
            "Module descriptor for package fragments has already been set: $moduleDescriptorForPackageFragments"
        }
        moduleDescriptorForPackageFragments = moduleDescriptor
    }

    private var moduleDescriptorForPackageFragments: ModuleDescriptor? = null

    private val klibMetadataModuleDescriptorFactory by lazy {
        KlibMetadataModuleDescriptorFactoryImpl(
            MetadataFactories.DefaultDescriptorFactory,
            MetadataFactories.DefaultPackageFragmentsFactory,
            MetadataFactories.flexibleTypeDeserializer
        )
    }

    private fun packageFragmentProviderForKotlinLibrary(
        library: KotlinLibrary
    ): PackageFragmentProvider {
        val languageVersionSettings = configuration.languageVersionSettings

        val moduleHeader = parseModuleHeader(library.moduleHeaderData)
        val moduleName = Name.special(moduleHeader.moduleName)
        val moduleOrigin = DeserializedKlibModuleOrigin(library)
        val libraryModuleDescriptor = MetadataFactories.DefaultDescriptorFactory.createDescriptor(
            moduleName, storageManager, builtIns, moduleOrigin
        )
        val packageFragmentNames = moduleHeader.packageFragmentNameList
        libraryModuleDescriptor.setDependencies(listOf(libraryModuleDescriptor, builtIns.builtInsModule))

        return klibMetadataModuleDescriptorFactory.createPackageFragmentProvider(
            library,
            packageAccessHandler = null,
            packageFragmentNames = packageFragmentNames,
            storageManager = LockBasedStorageManager("KlibMetadataPackageFragmentProvider"),
            moduleDescriptor = moduleDescriptorForPackageFragments ?: libraryModuleDescriptor,
            configuration = CompilerDeserializationConfiguration(languageVersionSettings),
            compositePackageFragmentAddend = null
        ).also {
            libraryModuleDescriptor.initialize(it)
        }
    }

    override fun packageFragmentProvider(): PackageFragmentProvider =
        CompositePackageFragmentProvider(kotlinLibraries().map(::packageFragmentProviderForKotlinLibrary))
}

private val MetadataFactories =
    KlibMetadataFactories(
        { storageManager -> DefaultBuiltIns.Instance },
        org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer
    )