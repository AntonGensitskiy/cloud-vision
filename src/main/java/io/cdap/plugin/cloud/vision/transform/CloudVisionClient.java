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

import com.google.api.client.util.Strings;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.CropHintsParams;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.ImageContext;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.ProductSearchParams;
import com.google.cloud.vision.v1.WebDetectionParams;
import io.cdap.plugin.cloud.vision.CredentialsHelper;
import io.cdap.plugin.cloud.vision.exception.CloudVisionExecutionException;
import java.util.Collections;
import javax.annotation.Nullable;

/**
 * Provides handy methods to access Google Cloud Vision.
 */
public class CloudVisionClient {

  private static final int SINGLE_RESPONSE_INDEX = 0;

  private final ImageExtractorTransformConfig config;

  public CloudVisionClient(ImageExtractorTransformConfig config) {
    this.config = config;
  }

  public AnnotateImageResponse extractFeature(String gcsPath) throws Exception {
    String serviceAccountFilePath = config.getServiceAccountFilePath();
    Credentials credentials = serviceAccountFilePath == null ? null
      : CredentialsHelper.getCredentials(serviceAccountFilePath);
    ImageAnnotatorSettings.Builder imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder();
    if (credentials != null) {
      imageAnnotatorSettings.setCredentialsProvider(FixedCredentialsProvider.create(credentials));
    }
    try (ImageAnnotatorClient client = ImageAnnotatorClient.create(imageAnnotatorSettings.build())) {
      ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
      Image img = Image.newBuilder().setSource(imgSource).build();
      Feature.Type featureType = config.getImageFeature().getFeatureType();
      Feature feature = Feature.newBuilder().setType(featureType).build();
      AnnotateImageRequest.Builder request = AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(img);
      ImageContext imageContext = getImageContext();
      if (imageContext != null) {
        request.setImageContext(imageContext);
      }

      BatchAnnotateImagesResponse response = client.batchAnnotateImages(Collections.singletonList(request.build()));
      AnnotateImageResponse annotateImageResponse = response.getResponses(SINGLE_RESPONSE_INDEX);
      if (annotateImageResponse.hasError()) {
        String errorMessage = String.format("Unable to extract '%s' feature of image '%s' due to: '%s'", featureType,
          gcsPath, annotateImageResponse.getError().getMessage());
        throw new CloudVisionExecutionException(errorMessage);
      }

      return annotateImageResponse;
    }
  }

  @Nullable
  private ImageContext getImageContext() {
    switch (config.getImageFeature()) {
      case TEXT:
        return Strings.isNullOrEmpty(config.getLanguageHints()) ? null
          : ImageContext.newBuilder().addAllLanguageHints(config.getLanguages()).build();
      case PRODUCT_SEARCH:
        ProductSearchParams.Builder productSearchParams = ProductSearchParams.newBuilder()
          .setProductSet(config.getProductSet())
          .addProductCategories(config.getProductCategory().getName());
        if (!Strings.isNullOrEmpty(config.getFilter())) {
          productSearchParams.setFilter(config.getFilter());
        }
        if (!Strings.isNullOrEmpty(config.getBoundingPolygon())) {
          productSearchParams.setBoundingPoly(config.getBoundingPoly());
        }
        return ImageContext.newBuilder().setProductSearchParams(productSearchParams).build();
      case CROP_HINTS:
        if (Strings.isNullOrEmpty(config.getAspectRatios())) {
          return null;
        }
        CropHintsParams cropHintsParams = CropHintsParams.newBuilder()
          .addAllAspectRatios(config.getAspectRatiosList())
          .build();
        return ImageContext.newBuilder().setCropHintsParams(cropHintsParams).build();
      case WEB_DETECTION:
        WebDetectionParams webDetectionParams = WebDetectionParams.newBuilder()
          .setIncludeGeoResults(config.isIncludeGeoResults())
          .build();
        return ImageContext.newBuilder().setWebDetectionParams(webDetectionParams).build();
      default:
        return null;
    }
  }
}
