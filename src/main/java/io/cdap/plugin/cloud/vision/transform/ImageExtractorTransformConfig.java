/*
 * Copyright © 2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin.cloud.vision.transform;

import com.google.cloud.vision.v1.BoundingPoly;
import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.cloud.vision.CloudVisionConfig;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Defines a {@link PluginConfig} that Image Extractor transform can use.
 */
public class ImageExtractorTransformConfig extends CloudVisionConfig {

  public static final Schema ERROR_SCHEMA = Schema.recordOf("error",
    Schema.Field.of("error", Schema.of(Schema.Type.STRING)));

  @Name(ImageExtractorConstants.PATH_FIELD)
  @Description("Field in the input schema containing the path to the image.")
  @Macro
  private String pathField;

  @Name(ImageExtractorConstants.OUTPUT_FIELD)
  @Description("Field to store the extracted image features. If the specified output field name already exists " +
    "in the input record, it will be overwritten.")
  @Macro
  private String outputField;

  @Name(ImageExtractorConstants.FEATURES)
  @Description("Features to extract from images.")
  @Macro
  private String features;

  @Name(ImageExtractorConstants.LANGUAGE_HINTS)
  @Description("Hints to detect the language of the text in the images.")
  @Macro
  @Nullable
  private String languageHints;

  @Name(ImageExtractorConstants.ASPECT_RATIOS)
  @Description("Ratio of the width to the height of the image. If not specified, the best possible crop is returned.")
  @Macro
  @Nullable
  private String aspectRatios;

  @Name(ImageExtractorConstants.INCLUDE_GEO_RESULTS)
  @Description("Whether to include results derived from the geo information in the image.")
  @Macro
  @Nullable
  private Boolean includeGeoResults;

  @Name(ImageExtractorConstants.PRODUCT_SET)
  @Description("Resource name of a ProductSet to be searched for similar images.")
  @Macro
  @Nullable
  private String productSet;

  @Name(ImageExtractorConstants.PRODUCT_CATEGORIES)
  @Description("List of product categories to search in.")
  @Macro
  @Nullable
  private String productCategories;

  @Name(ImageExtractorConstants.BOUNDING_POLYGON)
  @Description("Bounding polygon for the image detection.")
  @Macro
  @Nullable
  private String boundingPolygon;

  @Name(ImageExtractorConstants.FILTER)
  @Description("Filtering expression to restrict search results based on Product labels.")
  @Macro
  @Nullable
  private String filter;

  @Name(ImageExtractorConstants.SCHEMA)
  @Description("Schema of records output by the transform.")
  @Nullable
  private String schema;

  public ImageExtractorTransformConfig(String project, String serviceFilePath, String pathField, String outputField,
                                       String features, @Nullable String languageHints, @Nullable String aspectRatios,
                                       @Nullable Boolean includeGeoResults, @Nullable String productSet,
                                       @Nullable String productCategories, @Nullable String boundingPolygon,
                                       @Nullable String filter, @Nullable String schema) {
    super(project, serviceFilePath);
    this.pathField = pathField;
    this.outputField = outputField;
    this.features = features;
    this.languageHints = languageHints;
    this.aspectRatios = aspectRatios;
    this.includeGeoResults = includeGeoResults;
    this.productSet = productSet;
    this.productCategories = productCategories;
    this.boundingPolygon = boundingPolygon;
    this.filter = filter;
    this.schema = schema;
  }

  public String getPathField() {
    return pathField;
  }

  public String getOutputField() {
    return outputField;
  }

  public String getFeatures() {
    return features;
  }

  @Nullable
  public String getLanguageHints() {
    return languageHints;
  }

  public List<String> getLanguages() {
    return Strings.isNullOrEmpty(languageHints) ? Collections.emptyList() : Arrays.asList(languageHints.split(","));
  }

  @Nullable
  public String getSchema() {
    return schema;
  }

  @Nullable
  public String getProductSet() {
    return productSet;
  }

  @Nullable
  public String getProductCategories() {
    return productCategories;
  }

  @Nullable
  public String getFilter() {
    return filter;
  }

  public ProductCategory getProductCategory() {
    return Objects.requireNonNull(ProductCategory.fromDisplayName(productCategories));
  }

  public ImageFeature getImageFeature() {
    return Objects.requireNonNull(ImageFeature.fromDisplayName(features));
  }

  @Nullable
  public String getAspectRatios() {
    return aspectRatios;
  }

  public List<Float> getAspectRatiosList() {
    if (Strings.isNullOrEmpty(aspectRatios)) {
      return Collections.emptyList();
    }

    return Arrays.stream(aspectRatios.split(","))
      .map(Float::valueOf)
      .collect(Collectors.toList());
  }

  @Nullable
  public Boolean isIncludeGeoResults() {
    return includeGeoResults;
  }

  @Nullable
  public String getBoundingPolygon() {
    return boundingPolygon;
  }

  @Nullable
  public BoundingPoly getBoundingPoly() {
    if (Strings.isNullOrEmpty(boundingPolygon)) {
      return null;
    }

    BoundingPoly.Builder builder = BoundingPoly.newBuilder();
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(boundingPolygon, builder);
      return builder.build();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(String.format("Could not parse schema string: '%s'", schema), e);
    }
  }

  /**
   * Parses the json representation into a schema object.
   *
   * @return parsed schema object of json representation.
   * @throws RuntimeException if there was an exception parsing the schema.
   */
  @Nullable
  public Schema getParsedSchema() {
    try {
      return Strings.isNullOrEmpty(schema) ? null : Schema.parseJson(schema);
    } catch (IOException e) {
      // this should not happen, since schema string comes from UI
      throw new IllegalStateException(String.format("Could not parse schema string: '%s'", schema), e);
    }
  }

  /**
   * Validates {@link ImageExtractorTransformConfig} instance.
   *
   * @param collector failure collector.
   */
  public void validate(FailureCollector collector) {
    if (!containsMacro(ImageExtractorConstants.PATH_FIELD) && Strings.isNullOrEmpty(pathField)) {
      collector.addFailure("Path field must be specified", null)
        .withConfigProperty(ImageExtractorConstants.PATH_FIELD);
    }
    if (!containsMacro(ImageExtractorConstants.OUTPUT_FIELD) && Strings.isNullOrEmpty(outputField)) {
      collector.addFailure("Output field must be specified", null)
        .withConfigProperty(ImageExtractorConstants.OUTPUT_FIELD);
    }
    if (!containsMacro(ImageExtractorConstants.FEATURES)) {
      if (Strings.isNullOrEmpty(features)) {
        collector.addFailure("Features must be specified", null)
          .withConfigProperty(ImageExtractorConstants.FEATURES);
      } else if (ImageFeature.fromDisplayName(features) == null) {
        collector.addFailure("Invalid image feature name", null)
          .withConfigProperty(ImageExtractorConstants.FEATURES);
      }
    }
    if (!containsMacro(ImageExtractorConstants.BOUNDING_POLYGON) && !Strings.isNullOrEmpty(boundingPolygon)) {
      try {
        getBoundingPoly();
      } catch (IllegalStateException e) {
        collector.addFailure("Could not parse bounding polygon string.", null)
          .withConfigProperty(ImageExtractorConstants.BOUNDING_POLYGON);
      }
    }
  }

  /**
   * Validate that the provided schema is compatible with the inferred schema. The provided schema is compatible if
   * every field is compatible with the corresponding field in the inferred schema. A field is compatible if it is of
   * the same type or is a nullable version of that type. It is assumed that both schemas are record schemas.
   *
   * @param inferredSchema the inferred schema
   * @param providedSchema the provided schema to check compatibility
   * @param collector      failure collector
   * @throws IllegalArgumentException if the schemas are not type compatible
   */
  public static void validateFieldsMatch(Schema inferredSchema, Schema providedSchema, FailureCollector collector) {
    for (Schema.Field field : providedSchema.getFields()) {
      Schema.Field inferredField = inferredSchema.getField(field.getName());
      Schema inferredFieldSchema = inferredField.getSchema();
      Schema providedFieldSchema = field.getSchema();

      boolean isInferredFieldNullable = inferredFieldSchema.isNullable();
      boolean isProvidedFieldNullable = providedFieldSchema.isNullable();

      Schema inferredFieldNonNullableSchema = isInferredFieldNullable
        ? inferredFieldSchema.getNonNullable() : inferredFieldSchema;
      Schema providedFieldNonNullableSchema = isProvidedFieldNullable ?
        providedFieldSchema.getNonNullable() : providedFieldSchema;

      Schema.Type inferredType = inferredFieldNonNullableSchema.getType();
      Schema.LogicalType inferredLogicalType = inferredFieldNonNullableSchema.getLogicalType();
      Schema.Type providedType = providedFieldNonNullableSchema.getType();
      Schema.LogicalType providedLogicalType = providedFieldNonNullableSchema.getLogicalType();
      if (inferredType != providedType && inferredLogicalType != providedLogicalType) {
        String errorMessage = String.format("Expected field '%s' to be of type '%s', but it is of type '%s'",
          field.getName(), inferredFieldNonNullableSchema.getDisplayName(),
          providedFieldNonNullableSchema.getDisplayName());

        collector.addFailure(errorMessage, String.format("Change field '%s' to be a supported type", field.getName()))
          .withOutputSchemaField(field.getName(), null);
      }

      if (!isInferredFieldNullable && isProvidedFieldNullable) {
        String errorMessage = String.format("Field '%s' should not be nullable", field.getName());
        collector.addFailure(errorMessage, String.format("Change field '%s' to be non-nullable", field.getName()))
          .withOutputSchemaField(field.getName(), null);
      }
    }
  }
}
