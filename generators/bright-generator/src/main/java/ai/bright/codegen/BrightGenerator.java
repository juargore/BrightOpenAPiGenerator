package ai.bright.codegen;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.*;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.*;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.text.WordUtils;

import static org.openapitools.codegen.utils.StringUtils.*;

public class BrightGenerator extends DefaultCodegen implements CodegenConfig {

  public enum SERIALIZATION_LIBRARY_TYPE { multiplatform }
  protected static final String MULTIPLATFORM = "multiplatform";
  public static final String MODEL_MUTABLE = "modelMutable";
  public static final String COLLECTION_TYPE = "collectionType";
  protected static final String VENDOR_EXTENSION_BASE_NAME_LITERAL = "x-base-name-literal";
  public static final String USE_ANDROID_MAVEN_GRADLE_PLUGIN = "useAndroidMavenGradlePlugin";
  public static final String ANDROID_GRADLE_VERSION = "androidGradleVersion";
  public static final String ANDROID_SDK_VERSION = "androidSdkVersion";
  public static final String ANDROID_BUILD_TOOLS_VERSION = "androidBuildToolsVersion";

  protected Boolean useAndroidMavenGradlePlugin = true;
  protected String androidGradleVersion;
  protected String androidSdkVersion;
  protected String androidBuildToolsVersion;
  protected String gradleWrapperPackage = "gradle.wrapper";

  private final Logger LOGGER = LoggerFactory.getLogger(BrightGenerator.class);
  protected String collectionType = CollectionType.LIST.value;

  protected String artifactId;
  protected String artifactVersion = "0.0.1";
  protected String groupId = "ai.bright";
  protected String packageName = "ai.bright";
  protected String apiSuffix = "Api";

  protected String sourceFolder = "src/";
  protected String testFolder = "src/test/";
  protected String commonMainSourceFolder = "/src/commonMain/kotlin/";
  protected String androidMainSourceFolder = "/src/androidMain/kotlin/";
  protected String iosMainSourceFolder = "/src/iosMain/kotlin/";

  protected String apiDocPath = "docs/";
  protected String modelDocPath = "docs/";
  protected boolean parcelizeModels = false;
  protected boolean serializableModel = false;
  protected boolean needsDataClassBody = false;
  protected boolean nonPublicApi = false;

  protected CodegenConstants.ENUM_PROPERTY_NAMING_TYPE enumPropertyNaming = CodegenConstants.ENUM_PROPERTY_NAMING_TYPE.camelCase;
  protected SERIALIZATION_LIBRARY_TYPE serializationLibrary = SERIALIZATION_LIBRARY_TYPE.multiplatform;

  public enum CollectionType {
    ARRAY("array"),
    LIST("list");

    public final String value;

    CollectionType(String value) {
      this.value = value;
    }
  }

  public BrightGenerator() {
    super();

    supportsInheritance = true;
    setSortModelPropertiesByRequiredFlag(true);

    this.templateDir = "bright";
    modelTemplateFiles.put("model.mustache", ".kt");
    modelDocTemplateFiles.put("model_doc.mustache", ".md");
    apiTemplateFiles.put("api.mustache", ".kt");
    apiDocTemplateFiles.put("api_doc.mustache", ".md");

    languageSpecificPrimitives = new HashSet<String>(Arrays.asList(
            "kotlin.Byte",
            "kotlin.ByteArray",
            "kotlin.Short",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Boolean",
            "kotlin.Char",
            "kotlin.String",
            "kotlin.Array",
            "kotlin.collections.List",
            "kotlin.collections.Map",
            "kotlin.collections.Set"
    ));

    reservedWords = new HashSet<String>(Arrays.asList(
            "as", "break", "class", "continue",
            "do", "else", "false", "for",
            "fun", "if", "in", "interface",
            "is", "null", "object", "package",
            "return", "super", "this", "throw",
            "true", "try", "typealias", "typeof",
            "val", "var", "when", "while",
            "private", "open", "external", "internal"
    ));

    defaultIncludes = new HashSet<String>(Arrays.asList(
            "kotlin.Byte",
            "kotlin.ByteArray",
            "kotlin.Byte.Array",
            "kotlin.Short",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Boolean",
            "kotlin.Char",
            "kotlin.Array",
            "kotlin.collections.List",
            "kotlin.collections.Set",
            "kotlin.collections.Map"
    ));

    typeMapping = new HashMap<String, String>();
    typeMapping.put("string", "kotlin.String");
    typeMapping.put("boolean", "kotlin.Boolean");
    typeMapping.put("integer", "kotlin.Int");
    typeMapping.put("float", "kotlin.Float");
    typeMapping.put("long", "kotlin.Long");
    typeMapping.put("double", "kotlin.Double");
    typeMapping.put("ByteArray", "kotlin.ByteArray");
    typeMapping.put("number", "java.math.BigDecimal");
    typeMapping.put("decimal", "java.math.BigDecimal");
    typeMapping.put("date-time", "java.time.OffsetDateTime");
    typeMapping.put("date", "java.time.LocalDate");
    typeMapping.put("file", "java.io.File");
    typeMapping.put("array", "kotlin.Array");
    typeMapping.put("list", "kotlin.collections.List");
    typeMapping.put("set", "kotlin.collections.Set");
    typeMapping.put("map", "kotlin.collections.Map");
    typeMapping.put("object", "kotlin.Any");
    typeMapping.put("binary", "kotlin.ByteArray");
    typeMapping.put("Date", "java.time.LocalDate");
    typeMapping.put("DateTime", "kotlin.String");

    instantiationTypes.put("array", "kotlin.collections.ArrayList");
    instantiationTypes.put("list", "kotlin.collections.ArrayList");
    instantiationTypes.put("map", "kotlin.collections.HashMap");

    importMapping = new HashMap<String, String>();
    importMapping.put("BigDecimal", "java.math.BigDecimal");
    importMapping.put("UUID", "java.util.String");
    importMapping.put("URI", "java.net.URI");
    importMapping.put("File", "java.io.File");
    importMapping.put("Date", "java.time.LocalDate");
    importMapping.put("Timestamp", "java.sql.Timestamp");
    importMapping.put("DateTime", "java.time.OffsetDateTime");
    importMapping.put("LocalDateTime", "java.time.LocalDateTime");
    importMapping.put("LocalDate", "java.time.LocalDate");
    importMapping.put("LocalTime", "java.time.LocalTime");

    specialCharReplacements.put(";", "Semicolon");

    cliOptions.clear();
    addOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC, sourceFolder);
    addOption(CodegenConstants.PACKAGE_NAME, "Generated artifact package name.", packageName);
    addOption(CodegenConstants.API_SUFFIX, CodegenConstants.API_SUFFIX_DESC, apiSuffix);
    addOption(CodegenConstants.GROUP_ID, "Generated artifact package's organization (i.e. maven groupId).", groupId);
    addOption(CodegenConstants.ARTIFACT_ID, "Generated artifact id (name of jar).", artifactId);
    addOption(CodegenConstants.ARTIFACT_VERSION, "Generated artifact's package version.", artifactVersion);

    CliOption collectionType = new CliOption(COLLECTION_TYPE, "Option. Collection type to use");
    Map<String, String> collectionOptions = new HashMap<>();
    collectionOptions.put(CollectionType.ARRAY.value, "kotlin.Array");
    collectionOptions.put(CollectionType.LIST.value, "kotlin.collections.List");
    collectionType.setEnum(collectionOptions);
    collectionType.setDefault(this.collectionType);
    cliOptions.add(collectionType);

    CliOption enumPropertyNamingOpt = new CliOption(CodegenConstants.ENUM_PROPERTY_NAMING, CodegenConstants.ENUM_PROPERTY_NAMING_DESC);
    cliOptions.add(enumPropertyNamingOpt.defaultValue(enumPropertyNaming.name()));

    CliOption serializationLibraryOpt = new CliOption(CodegenConstants.SERIALIZATION_LIBRARY, "");
    cliOptions.add(serializationLibraryOpt.defaultValue(serializationLibrary.name()));

    cliOptions.add(new CliOption(CodegenConstants.PARCELIZE_MODELS, CodegenConstants.PARCELIZE_MODELS_DESC));
    cliOptions.add(new CliOption(CodegenConstants.SERIALIZABLE_MODEL, CodegenConstants.SERIALIZABLE_MODEL_DESC));
    cliOptions.add(new CliOption(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG, CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG_DESC));
    cliOptions.add(new CliOption(CodegenConstants.SORT_MODEL_PROPERTIES_BY_REQUIRED_FLAG, CodegenConstants.SORT_MODEL_PROPERTIES_BY_REQUIRED_FLAG_DESC));

    cliOptions.add(CliOption.newBoolean(MODEL_MUTABLE, "", false));

    cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
    cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
    cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, CodegenConstants.INVOKER_PACKAGE_DESC));
    cliOptions.add(new CliOption(CodegenConstants.GROUP_ID, "groupId for use in the generated build.gradle and pom.xml"));
    cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_ID, "artifactId for use in the generated build.gradle and pom.xml"));
    cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_VERSION, "artifact version for use in the generated build.gradle and pom.xml"));
    cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC));
    cliOptions.add(CliOption.newBoolean(USE_ANDROID_MAVEN_GRADLE_PLUGIN, "A flag to toggle android-maven gradle plugin.")
            .defaultValue(Boolean.TRUE.toString()));
    cliOptions.add(new CliOption(ANDROID_GRADLE_VERSION, "gradleVersion version for use in the generated build.gradle"));
    cliOptions.add(new CliOption(ANDROID_SDK_VERSION, "compileSdkVersion version for use in the generated build.gradle"));
    cliOptions.add(new CliOption(ANDROID_BUILD_TOOLS_VERSION, "buildToolsVersion version for use in the generated build.gradle"));
  }

  public String getName() { return "bright"; }

  @Override
  public Map<String, Object> postProcessModels(Map<String, Object> objs) {
    objs = super.postProcessModelsEnum(objs);
    List<Object> models = (List<Object>) objs.get("models");
    for (Object _mo : models) {
      Map<String, Object> mo = (Map<String, Object>) _mo;
      CodegenModel cm = (CodegenModel) mo.get("model");
      if (cm.getDiscriminator() != null) {
        cm.vendorExtensions.put("x-has-data-class-body", true);
        break;
      }

      for (CodegenProperty var : cm.vars) {
        if (var.isEnum || isSerializableModel()) {
          cm.vendorExtensions.put("x-has-data-class-body", true);
          break;
        }
      }
    }
    return postProcessModelsEnum(objs);
  }

  @Override
  public String getTypeDeclaration(Schema p) {
    Schema<?> schema = ModelUtils.unaliasSchema(this.openAPI, p, importMapping);
    Schema<?> target = ModelUtils.isGenerateAliasAsModel() ? p : schema;
    if (ModelUtils.isArraySchema(target)) {
      Schema<?> items = getSchemaItems((ArraySchema) schema);
      return "kotlin.collections.List" + "<" + getTypeDeclaration(items) + ">";
    }
    return super.getTypeDeclaration(target);
  }

  @Override
  public void processOpts() {
    super.processOpts();

    if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
      this.setPackageName((String) additionalProperties.get(CodegenConstants.PACKAGE_NAME));
      if (!additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
        this.setModelPackage(packageName + ".models");
      }

      if (!additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
        this.setApiPackage(packageName + ".apis");
      }
    } else {
      additionalProperties.put(CodegenConstants.PACKAGE_NAME, packageName);
    }

    // commonMain files generated
    final String infrastructureFolder = (commonMainSourceFolder + packageName + ".infrastructure").replace(".", File.separator);
    final String authFolder = (commonMainSourceFolder + packageName + ".auth").replace(".", File.separator);
    final String networkFolder = (commonMainSourceFolder + packageName + ".network").replace(".", File.separator);
    final String typesFolder = (commonMainSourceFolder + packageName + ".types").replace(".", File.separator);

    // androidMain & iosMain files generated
    final String androidNetworkFolder = (androidMainSourceFolder + packageName + ".network").replace(".", File.separator);
    final String androidTypesFolder = (androidMainSourceFolder + packageName + ".types").replace(".", File.separator);
    final String iosNetworkFolder = (iosMainSourceFolder + packageName + ".network").replace(".", File.separator);
    final String iosTypesFolder = (iosMainSourceFolder + packageName + ".types").replace(".", File.separator);
    final String iosUtilsFolder = (iosMainSourceFolder + packageName + ".utils").replace(".", File.separator);

    if (additionalProperties.containsKey(COLLECTION_TYPE)) {
      setCollectionType(additionalProperties.get(COLLECTION_TYPE).toString());
    }

    if (additionalProperties.containsKey(USE_ANDROID_MAVEN_GRADLE_PLUGIN)) {
      this.setUseAndroidMavenGradlePlugin(Boolean.valueOf((String) additionalProperties
              .get(USE_ANDROID_MAVEN_GRADLE_PLUGIN)));
    } else {
      additionalProperties.put(USE_ANDROID_MAVEN_GRADLE_PLUGIN, useAndroidMavenGradlePlugin);
    }

    if (additionalProperties.containsKey(ANDROID_GRADLE_VERSION)) {
      this.setAndroidGradleVersion((String) additionalProperties.get(ANDROID_GRADLE_VERSION));
    }

    if (additionalProperties.containsKey(ANDROID_SDK_VERSION)) {
      this.setAndroidSdkVersion((String) additionalProperties.get(ANDROID_SDK_VERSION));
    }

    if (additionalProperties.containsKey(ANDROID_BUILD_TOOLS_VERSION)) {
      this.setAndroidBuildToolsVersion((String) additionalProperties.get(ANDROID_BUILD_TOOLS_VERSION));
    }

    if (additionalProperties.containsKey(CodegenConstants.LIBRARY)) {
      this.setLibrary((String) additionalProperties.get(CodegenConstants.LIBRARY));
    }

    additionalProperties.put(this.serializationLibrary.name(), true);
    additionalProperties.put("sourceFolder", sourceFolder);
    additionalProperties.put("apiDocPath", apiDocPath);
    additionalProperties.put("modelDocPath", modelDocPath);

    if (CollectionType.LIST.value.equals(collectionType)) {
      typeMapping.put("array", "kotlin.collections.List");
      typeMapping.put("list", "kotlin.collections.List");
      additionalProperties.put("isList", true);
    }

    processMultiplatformLibrary(
            infrastructureFolder,
            authFolder,
            networkFolder,
            typesFolder,
            androidNetworkFolder,
            androidTypesFolder,
            iosNetworkFolder,
            iosTypesFolder,
            iosUtilsFolder
    );
  }

  private void processMultiplatformLibrary(
          final String infrastructureFolder,
          final String authFolder,
          final String networkFolder,
          final String typesFolder,
          final String androidNetworkFolder,
          final String androidTypesFolder,
          final String iosNetworkFolder,
          final String iosTypesFolder,
          final String iosUtilsFolder
  ) {
    commonJvmMultiplatformSupportingFiles(infrastructureFolder);
    additionalProperties.put(MULTIPLATFORM, true);

    // multiplatform default includes
    defaultIncludes.add(packageName + ".infrastructure.Base64ByteArray");
    defaultIncludes.add(packageName + ".infrastructure.OctetByteArray");

    // multiplatform type mapping
    typeMapping.put("number", "kotlin.Double");
    typeMapping.put("file", "kotlin.ByteArray");
    typeMapping.put("binary", "OctetByteArray");
    typeMapping.put("ByteArray", "Base64ByteArray");
    typeMapping.put("KotlinString", "kotlin.String");
    typeMapping.put("KotlinInt", "kotlin.Int");
    typeMapping.put("KotlinDouble", "kotlin.Double");
    typeMapping.put("KotlinAny", "kotlin.Any");
    typeMapping.put("AnyType", "kotlin.Any");
    typeMapping.put("object", "kotlin.String");
    typeMapping.put("UUID", "kotlin.String");
    typeMapping.put("kotlin.Byte.Array", "kotlin.ByteArray");

    // multiplatform import mapping
    importMapping.put("BigDecimal", "kotlin.Double");
    importMapping.put("UUID", "kotlin.String");
    importMapping.put("URI", "kotlin.String");
    importMapping.put("InputProvider", "kotlin.ByteArray");
    importMapping.put("File", packageName + ".infrastructure.OctetByteArray");
    importMapping.put("Timestamp", "kotlin.String");
    importMapping.put("LocalDateTime", "kotlin.String");
    importMapping.put("LocalDate", "kotlin.String");
    importMapping.put("LocalTime", "kotlin.String");
    importMapping.put("Base64ByteArray", packageName + ".infrastructure.Base64ByteArray");
    importMapping.put("OctetByteArray", packageName + ".infrastructure.OctetByteArray");
    importMapping.put("inline_response_200", packageName + ".models.InlineResponse200");
    importMapping.put("inline_response_503", packageName + ".models.InlineResponse503");
    importMapping.put("KotlinString", "kotlin.String");
    importMapping.put("KotlinInt", "kotlin.Int");
    importMapping.put("KotlinBoolean", "kotlin.Boolean");
    importMapping.put("KotlinDouble", "kotlin.Double");
    importMapping.put("KotlinCollectionsList", "kotlin.collections.List");
    importMapping.put("kotlinCollectionsList", "kotlin.collections.List");
    importMapping.put("KotlinCollectionsSet", "kotlin.collections.Set");
    importMapping.put("kotlinCollectionsSet", "kotlin.collections.Set");
    importMapping.put("KotlinCollectionsMap", "kotlin.collections.Map");
    importMapping.put("kotlinCollectionsMap", "kotlin.collections.Map");
    importMapping.put("map", "kotlin.collections.Map");
    importMapping.put("KotlinAny", "kotlin.Any");

    // multiplatform specific supporting files
    supportingFiles.add(new SupportingFile("infrastructure/Base64ByteArray.kt.mustache", infrastructureFolder, "Base64ByteArray.kt"));
    supportingFiles.add(new SupportingFile("infrastructure/Bytes.kt.mustache", infrastructureFolder, "Bytes.kt"));
    supportingFiles.add(new SupportingFile("infrastructure/HttpResponse.kt.mustache", infrastructureFolder, "HttpResponse.kt"));
    supportingFiles.add(new SupportingFile("infrastructure/OctetByteArray.kt.mustache", infrastructureFolder, "OctetByteArray.kt"));

    // network supporting files
    supportingFiles.add(new SupportingFile("network/MultipartFormData.kt.mustache", networkFolder, "MultipartFormData.kt"));
    supportingFiles.add(new SupportingFile("network/NetworkLogger.kt.mustache", networkFolder, "NetworkLogger.kt"));
    supportingFiles.add(new SupportingFile("network/Request.kt.mustache", networkFolder, "Request.kt"));
    supportingFiles.add(new SupportingFile("network/RequestConfig.kt.mustache", networkFolder, "RequestConfig.kt"));
    supportingFiles.add(new SupportingFile("network/RequestMethod.kt.mustache", networkFolder, "RequestMethod.kt"));
    supportingFiles.add(new SupportingFile("network/Response.kt.mustache", networkFolder, "Response.kt"));

    // android supporting files
    supportingFiles.add(new SupportingFile("android/RequestAndroid.kt.mustache", androidNetworkFolder, "RequestAndroid.kt"));
    supportingFiles.add(new SupportingFile("android/UUID.kt.mustache", androidTypesFolder, "UUID.kt"));

    // ios supporting files
    supportingFiles.add(new SupportingFile("ios/RequestIos.kt.mustache", iosNetworkFolder, "RequestIos.kt"));
    supportingFiles.add(new SupportingFile("ios/UUID.kt.mustache", iosTypesFolder, "UUID.kt"));
    supportingFiles.add(new SupportingFile("ios/Bytes.kt.mustache", iosUtilsFolder, "Bytes.kt"));

    // types supporting files
    supportingFiles.add(new SupportingFile("types/UUID.kt.mustache", typesFolder, "UUID.kt"));

    // multiplatform specific auth
    supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.kt.mustache", authFolder, "ApiKeyAuth.kt"));
    supportingFiles.add(new SupportingFile("auth/Authentication.kt.mustache", authFolder, "Authentication.kt"));
    supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.kt.mustache", authFolder, "HttpBasicAuth.kt"));
    supportingFiles.add(new SupportingFile("auth/HttpBearerAuth.kt.mustache", authFolder, "HttpBearerAuth.kt"));
    supportingFiles.add(new SupportingFile("auth/OAuth.kt.mustache", authFolder, "OAuth.kt"));

    // gradle wrapper files
    supportingFiles.add(new SupportingFile("gradlew.mustache", "", "gradlew"));
    supportingFiles.add(new SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"));
    supportingFiles.add(new SupportingFile("gradle-wrapper.properties.mustache", gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.properties"));
    supportingFiles.add(new SupportingFile("gradle/wrapper/gradle-wrapper.jar", gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.jar"));
  }

  private void commonJvmMultiplatformSupportingFiles(String infrastructureFolder) {
    supportingFiles.add(new SupportingFile("infrastructure/ApiClient.kt.mustache", infrastructureFolder, "ApiClient.kt"));
    supportingFiles.add(new SupportingFile("infrastructure/ApiAbstractions.kt.mustache", infrastructureFolder, "ApiAbstractions.kt"));
    supportingFiles.add(new SupportingFile("infrastructure/RequestConfig.kt.mustache", infrastructureFolder, "RequestConfig.kt"));
    supportingFiles.add(new SupportingFile("infrastructure/RequestMethod.kt.mustache", infrastructureFolder, "RequestMethod.kt"));
  }

  @Override
  public String getSchemaType(Schema p) {
    String openAPIType = super.getSchemaType(p);
    String type;
    String finalName = "";

    // This maps, for example, long -> kotlin.Long based on hashes in this type's constructor
    if (typeMapping.containsKey(openAPIType)) {
      type = typeMapping.get(openAPIType);

      if (languageSpecificPrimitives.contains(type)) {
        String modelName = WordUtils.uncapitalize(toModelName(type));

        Object[] objectsInKotlin = languageSpecificPrimitives.toArray();
        String[] primitivesInKotlin = new String[objectsInKotlin.length];
        for( int i = 0; i< objectsInKotlin.length; i ++) {
          primitivesInKotlin[i] = String.valueOf(objectsInKotlin[i]).replace(".", "");
        }

        boolean contains = Arrays.stream(primitivesInKotlin).anyMatch(modelName::equals);
        if (contains) {
          for (String w : modelName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
            finalName += w + ".";
          }

          StringBuffer sb = new StringBuffer(finalName);
          sb.deleteCharAt(sb.length() - 1);
          finalName = WordUtils.uncapitalize(String.valueOf(sb));
        } else {
          finalName = modelName;
        }

        return finalName;
      }
    } else {
      type = openAPIType;
    }
    return toModelName(type);
  }

  // override with any special text escaping logic
  @Override
  @SuppressWarnings("static-method")
  public String escapeText(String input) {
    if (input == null) {
      return input;
    }

    return escapeUnsafeCharacters(
            StringEscapeUtils.unescapeJava(
                    StringEscapeUtils.escapeJava(input)
                            .replace("\\/", "/"))
                    .replaceAll("[\\t\\n\\r]", " ")
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\""));
  }

  @Override
  public String escapeUnsafeCharacters(String input) {
    return input.replace("*/", "*_/").replace("/*", "/_*");
  }

  @Override
  public String escapeQuotationMark(String input) {
    return input.replace("\"", "");
  }

  @Override
  public String apiFileFolder() {
    // generated-apis/
    return (outputFolder + File.separator + commonMainSourceFolder + apiPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
  }

  @Override
  public String apiDocFileFolder() {
    return (outputFolder + File.separator + commonMainSourceFolder + apiDocPath).replace('/', File.separatorChar);
  }

  @Override
  public String modelFileFolder() {
    return outputFolder + File.separator + commonMainSourceFolder + modelPackage().replace('.', File.separatorChar);
  }

  @Override
  public String modelDocFileFolder() {
    return (outputFolder + commonMainSourceFolder + modelDocPath).replace('/', File.separatorChar);
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public boolean isSerializableModel() {
    return serializableModel;
  }

  public void setCollectionType(String collectionType) {
    this.collectionType = collectionType;
  }

  public Boolean getUseAndroidMavenGradlePlugin() {
    return useAndroidMavenGradlePlugin;
  }

  public String getAndroidGradleVersion() {
    return androidGradleVersion;
  }

  public String getAndroidSdkVersion() {
    return androidSdkVersion;
  }

  public String getAndroidBuildToolsVersion() {
    return androidBuildToolsVersion;
  }

  public void setUseAndroidMavenGradlePlugin(Boolean useAndroidMavenGradlePlugin) {
    this.useAndroidMavenGradlePlugin = useAndroidMavenGradlePlugin;
  }

  public void setAndroidGradleVersion(String androidGradleVersion) {
    this.androidGradleVersion = androidGradleVersion;
  }

  public void setAndroidSdkVersion(String androidSdkVersion) {
    this.androidSdkVersion = androidSdkVersion;
  }

  public void setAndroidBuildToolsVersion(String androidBuildToolsVersion) {
    this.androidBuildToolsVersion = androidBuildToolsVersion;
  }
}
