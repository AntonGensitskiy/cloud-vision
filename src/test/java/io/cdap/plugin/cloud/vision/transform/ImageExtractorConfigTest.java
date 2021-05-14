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
import com.google.protobuf.util.JsonFormat;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.cloud.vision.ValidationAssertions;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of {@link ImageExtractorTransformConfig} methods.
 */
public class ImageExtractorConfigTest {

  private static final String MOCK_STAGE = "mockstage";
  private static final Schema VALID_SCHEMA = Schema.recordOf(
    "schema",
    Schema.Field.of("path", Schema.of(Schema.Type.STRING)),
    Schema.Field.of("extracted", ImageFeature.FACE.getSchema())
  );

  private static final ImageExtractorTransformConfig VALID = ImageExtractorConfigBuilder.builder()
    .setPathField("path")
    .setOutputField("extracted")
    .setFeatures(ImageFeature.FACE.getDisplayName())
    .setSchema(VALID_SCHEMA.toString())
    .build();

  @Test
  public void testValidatePathFieldNull() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setPathField(null)
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.PATH_FIELD);
  }

  @Test
  public void testValidatePathFieldEmpty() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setPathField("")
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.PATH_FIELD);
  }

  @Test
  public void testValidateOutputFieldNull() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setOutputField(null)
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.OUTPUT_FIELD);
  }

  @Test
  public void testValidateOutputFieldEmpty() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setOutputField("")
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.OUTPUT_FIELD);
  }

  @Test
  public void testValidateFeatureNull() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setFeatures(null)
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.FEATURES);
  }

  @Test
  public void testValidateFeatureEmpty() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setFeatures("")
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.FEATURES);
  }

  @Test
  public void testValidateFeatureInvalid() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setFeatures("invalid-split-by")
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.FEATURES);
  }


  @Test
  public void testValidateBoundingPoly() throws Exception {
    String polyJson = "{ \"vertices\": [ " +
      "{ \"y\": 520 }, " +
      "{ \"x\": 2369, \"y\": 520 }, " +
      "{ \"x\": 2369, \"y\": 1729 }, " +
      "{ \"y\": 1729 } " +
      "] }";
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setBoundingPolygon(polyJson)
      .build();

    BoundingPoly.Builder builder = BoundingPoly.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(polyJson, builder);
    BoundingPoly expectedBoundingPoly = builder.build();

    BoundingPoly actualBoundingPoly = config.getBoundingPoly();
    Assert.assertNotNull(actualBoundingPoly);
    Assert.assertEquals(expectedBoundingPoly, actualBoundingPoly);
  }

  @Test
  public void testValidateBoundingPolyInvalid() {
    ImageExtractorTransformConfig config = ImageExtractorConfigBuilder.builder(VALID)
      .setBoundingPolygon("invalid json")
      .build();

    MockFailureCollector failureCollector = new MockFailureCollector(MOCK_STAGE);
    config.validate(failureCollector);
    ValidationAssertions.assertPropertyValidationFailed(failureCollector, ImageExtractorConstants.BOUNDING_POLYGON);
  }
}
