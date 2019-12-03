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

package io.cdap.plugin.cloud.vision.transform.transformer;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Word;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.cloud.vision.transform.ImageExtractorConstants;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Transforms handwriting annotation of specified {@link AnnotateImageResponse} to {@link StructuredRecord} according
 * to the specified schema.
 */
public class FullTextAnnotationsToRecordTransformer extends ImageAnnotationToRecordTransformer {

  public FullTextAnnotationsToRecordTransformer(Schema schema, String outputFieldName) {
    super(schema, outputFieldName);
  }

  @Override
  public StructuredRecord transform(StructuredRecord input, AnnotateImageResponse annotateImageResponse) {
    TextAnnotation annotation = annotateImageResponse.getFullTextAnnotation();
    return getOutputRecordBuilder(input)
      .set(outputFieldName, extractHandwritingAnnotation(annotation))
      .build();
  }

  private StructuredRecord extractHandwritingAnnotation(TextAnnotation annotation) {
    Schema hwSchema = getHandwritingAnnotationSchema();
    StructuredRecord.Builder builder = StructuredRecord.builder(hwSchema);
    if (hwSchema.getField(ImageExtractorConstants.FullTextAnnotation.TEXT_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.FullTextAnnotation.TEXT_FIELD_NAME, annotation.getText());
    }

    Schema.Field pagesField = hwSchema.getField(ImageExtractorConstants.FullTextAnnotation.PAGES_FIELD_NAME);
    if (pagesField != null) {
      Schema pageSchema = getComponentSchema(pagesField);
      List<StructuredRecord> pages = annotation.getPagesList().stream()
        .map(v -> extractPage(v, pageSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.FullTextAnnotation.PAGES_FIELD_NAME, pages);
    }

    return builder.build();
  }

  private StructuredRecord extractPage(Page page, Schema schema) {
    StructuredRecord.Builder builder = StructuredRecord.builder(schema);
    if (schema.getField(ImageExtractorConstants.TextPage.TEXT_FIELD_NAME) != null) {
      String pageText = page.getBlocksList().stream()
        .map(Block::getParagraphsList)
        .flatMap(List::stream)
        .map(Paragraph::getWordsList)
        .flatMap(List::stream)
        .map(Word::getSymbolsList)
        .flatMap(List::stream)
        .map(Symbol::getText)
        .collect(Collectors.joining());
      builder.set(ImageExtractorConstants.TextPage.TEXT_FIELD_NAME, pageText);
    }
    if (schema.getField(ImageExtractorConstants.TextPage.WIDTH_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextPage.WIDTH_FIELD_NAME, page.getWidth());
    }
    if (schema.getField(ImageExtractorConstants.TextPage.HEIGHT_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextPage.HEIGHT_FIELD_NAME, page.getHeight());
    }
    if (schema.getField(ImageExtractorConstants.TextPage.CONFIDENCE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextPage.CONFIDENCE_FIELD_NAME, page.getConfidence());
    }
    Schema.Field languagesField = schema.getField(ImageExtractorConstants.TextPage.DETECTED_LANGUAGES_FIELD_NAME);
    if (languagesField != null) {
      Schema langSchema = getComponentSchema(languagesField);
      List<StructuredRecord> languages = page.getProperty().getDetectedLanguagesList().stream()
        .map(l -> extractDetectedLanguage(l, langSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextPage.DETECTED_LANGUAGES_FIELD_NAME, languages);
    }
    if (schema.getField(ImageExtractorConstants.TextPage.DETECTED_BREAK_FIELD_NAME) != null) {
      String detectedBreak = page.getProperty().getDetectedBreak().getType().name();
      builder.set(ImageExtractorConstants.TextPage.DETECTED_BREAK_FIELD_NAME, detectedBreak);
    }
    Schema.Field blocksField = schema.getField(ImageExtractorConstants.TextPage.BLOCKS_FIELD_NAME);
    if (blocksField != null) {
      Schema blockSchema = getComponentSchema(blocksField);
      List<StructuredRecord> blocks = page.getBlocksList().stream()
        .map(v -> extractBlock(v, blockSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextPage.BLOCKS_FIELD_NAME, blocks);
    }

    return builder.build();
  }

  private StructuredRecord extractBlock(Block block, Schema schema) {
    StructuredRecord.Builder builder = StructuredRecord.builder(schema);
    if (schema.getField(ImageExtractorConstants.TextBlock.TEXT_FIELD_NAME) != null) {
      String blockText = block.getParagraphsList().stream()
        .map(Paragraph::getWordsList)
        .flatMap(List::stream)
        .map(Word::getSymbolsList)
        .flatMap(List::stream)
        .map(Symbol::getText)
        .collect(Collectors.joining());
      builder.set(ImageExtractorConstants.TextBlock.TEXT_FIELD_NAME, blockText);
    }
    if (schema.getField(ImageExtractorConstants.TextBlock.BLOCK_TYPE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextBlock.BLOCK_TYPE_FIELD_NAME, block.getBlockType().name());
    }
    if (schema.getField(ImageExtractorConstants.TextBlock.CONFIDENCE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextBlock.CONFIDENCE_FIELD_NAME, block.getConfidence());
    }
    Schema.Field languagesField = schema.getField(ImageExtractorConstants.TextBlock.DETECTED_LANGUAGES_FIELD_NAME);
    if (languagesField != null) {
      Schema langSchema = getComponentSchema(languagesField);
      List<StructuredRecord> languages = block.getProperty().getDetectedLanguagesList().stream()
        .map(l -> extractDetectedLanguage(l, langSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextBlock.DETECTED_LANGUAGES_FIELD_NAME, languages);
    }
    if (schema.getField(ImageExtractorConstants.TextBlock.DETECTED_BREAK_FIELD_NAME) != null) {
      String detectedBreak = block.getProperty().getDetectedBreak().getType().name();
      builder.set(ImageExtractorConstants.TextBlock.DETECTED_BREAK_FIELD_NAME, detectedBreak);
    }
    Schema.Field paragraphsField = schema.getField(ImageExtractorConstants.TextBlock.PARAGRAPHS_FIELD_NAME);
    if (paragraphsField != null) {
      Schema paragraphSchema = getComponentSchema(paragraphsField);
      List<StructuredRecord> paragraphs = block.getParagraphsList().stream()
        .map(v -> extractParagraph(v, paragraphSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextBlock.PARAGRAPHS_FIELD_NAME, paragraphs);
    }
    Schema.Field boxField = schema.getField(ImageExtractorConstants.TextBlock.BOUNDING_BOX_FIELD_NAME);
    if (boxField != null) {
      Schema vertexSchema = getComponentSchema(boxField);
      List<StructuredRecord> paragraphs = block.getBoundingBox().getVerticesList().stream()
        .map(v -> extractVertex(v, vertexSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextBlock.BOUNDING_BOX_FIELD_NAME, paragraphs);
    }

    return builder.build();
  }

  private StructuredRecord extractParagraph(Paragraph paragraph, Schema schema) {
    StructuredRecord.Builder builder = StructuredRecord.builder(schema);
    if (schema.getField(ImageExtractorConstants.TextParagraph.TEXT_FIELD_NAME) != null) {
      String paragraphText = paragraph.getWordsList().stream()
        .map(Word::getSymbolsList)
        .flatMap(List::stream)
        .map(Symbol::getText)
        .collect(Collectors.joining());
      builder.set(ImageExtractorConstants.TextParagraph.TEXT_FIELD_NAME, paragraphText);
    }
    if (schema.getField(ImageExtractorConstants.TextParagraph.CONFIDENCE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextParagraph.CONFIDENCE_FIELD_NAME, paragraph.getConfidence());
    }
    Schema.Field languagesField = schema.getField(ImageExtractorConstants.TextParagraph.DETECTED_LANGUAGES_FIELD_NAME);
    if (languagesField != null) {
      Schema langSchema = getComponentSchema(languagesField);
      List<StructuredRecord> languages = paragraph.getProperty().getDetectedLanguagesList().stream()
        .map(l -> extractDetectedLanguage(l, langSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextParagraph.DETECTED_LANGUAGES_FIELD_NAME, languages);
    }
    if (schema.getField(ImageExtractorConstants.TextParagraph.DETECTED_BREAK_FIELD_NAME) != null) {
      String detectedBreak = paragraph.getProperty().getDetectedBreak().getType().name();
      builder.set(ImageExtractorConstants.TextParagraph.DETECTED_BREAK_FIELD_NAME, detectedBreak);
    }
    Schema.Field wordsField = schema.getField(ImageExtractorConstants.TextParagraph.WORDS_FIELD_NAME);
    if (wordsField != null) {
      Schema wordSchema = getComponentSchema(wordsField);
      List<StructuredRecord> words = paragraph.getWordsList().stream()
        .map(v -> extractWord(v, wordSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextParagraph.WORDS_FIELD_NAME, words);
    }
    Schema.Field boxField = schema.getField(ImageExtractorConstants.TextParagraph.BOUNDING_BOX_FIELD_NAME);
    if (boxField != null) {
      Schema vertexSchema = getComponentSchema(boxField);
      List<StructuredRecord> paragraphs = paragraph.getBoundingBox().getVerticesList().stream()
        .map(v -> extractVertex(v, vertexSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextParagraph.BOUNDING_BOX_FIELD_NAME, paragraphs);
    }

    return builder.build();
  }

  private StructuredRecord extractWord(Word word, Schema schema) {
    StructuredRecord.Builder builder = StructuredRecord.builder(schema);
    if (schema.getField(ImageExtractorConstants.TextWord.TEXT_FIELD_NAME) != null) {
      String wordText = word.getSymbolsList().stream()
        .map(Symbol::getText)
        .collect(Collectors.joining());
      builder.set(ImageExtractorConstants.TextWord.TEXT_FIELD_NAME, wordText);
    }
    if (schema.getField(ImageExtractorConstants.TextWord.CONFIDENCE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextWord.CONFIDENCE_FIELD_NAME, word.getConfidence());
    }
    Schema.Field languagesField = schema.getField(ImageExtractorConstants.TextWord.DETECTED_LANGUAGES_FIELD_NAME);
    if (languagesField != null) {
      Schema langSchema = getComponentSchema(languagesField);
      List<StructuredRecord> languages = word.getProperty().getDetectedLanguagesList().stream()
        .map(l -> extractDetectedLanguage(l, langSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextWord.DETECTED_LANGUAGES_FIELD_NAME, languages);
    }
    if (schema.getField(ImageExtractorConstants.TextWord.DETECTED_BREAK_FIELD_NAME) != null) {
      String detectedBreak = word.getProperty().getDetectedBreak().getType().name();
      builder.set(ImageExtractorConstants.TextWord.DETECTED_BREAK_FIELD_NAME, detectedBreak);
    }
    Schema.Field symbolsField = schema.getField(ImageExtractorConstants.TextWord.SYMBOLS_FIELD_NAME);
    if (symbolsField != null) {
      Schema symbolSchema = getComponentSchema(symbolsField);
      List<StructuredRecord> words = word.getSymbolsList().stream()
        .map(v -> extractSymbol(v, symbolSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextWord.SYMBOLS_FIELD_NAME, words);
    }
    Schema.Field boxField = schema.getField(ImageExtractorConstants.TextWord.BOUNDING_BOX_FIELD_NAME);
    if (boxField != null) {
      Schema vertexSchema = getComponentSchema(boxField);
      List<StructuredRecord> paragraphs = word.getBoundingBox().getVerticesList().stream()
        .map(v -> extractVertex(v, vertexSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextWord.BOUNDING_BOX_FIELD_NAME, paragraphs);
    }

    return builder.build();
  }

  private StructuredRecord extractSymbol(Symbol symbol, Schema schema) {
    StructuredRecord.Builder builder = StructuredRecord.builder(schema);
    if (schema.getField(ImageExtractorConstants.TextSymbol.TEXT_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextSymbol.TEXT_FIELD_NAME, symbol.getText());
    }
    if (schema.getField(ImageExtractorConstants.TextSymbol.CONFIDENCE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.TextSymbol.CONFIDENCE_FIELD_NAME, symbol.getConfidence());
    }
    Schema.Field languagesField = schema.getField(ImageExtractorConstants.TextSymbol.DETECTED_LANGUAGES_FIELD_NAME);
    if (languagesField != null) {
      Schema langSchema = getComponentSchema(languagesField);
      List<StructuredRecord> languages = symbol.getProperty().getDetectedLanguagesList().stream()
        .map(l -> extractDetectedLanguage(l, langSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextSymbol.DETECTED_LANGUAGES_FIELD_NAME, languages);
    }
    if (schema.getField(ImageExtractorConstants.TextSymbol.DETECTED_BREAK_FIELD_NAME) != null) {
      String detectedBreak = symbol.getProperty().getDetectedBreak().getType().name();
      builder.set(ImageExtractorConstants.TextSymbol.DETECTED_BREAK_FIELD_NAME, detectedBreak);
    }
    Schema.Field boxField = schema.getField(ImageExtractorConstants.TextSymbol.BOUNDING_BOX_FIELD_NAME);
    if (boxField != null) {
      Schema vertexSchema = getComponentSchema(boxField);
      List<StructuredRecord> paragraphs = symbol.getBoundingBox().getVerticesList().stream()
        .map(v -> extractVertex(v, vertexSchema))
        .collect(Collectors.toList());
      builder.set(ImageExtractorConstants.TextSymbol.BOUNDING_BOX_FIELD_NAME, paragraphs);
    }

    return builder.build();
  }

  private StructuredRecord extractDetectedLanguage(TextAnnotation.DetectedLanguage language, Schema schema) {
    StructuredRecord.Builder builder = StructuredRecord.builder(schema);
    if (schema.getField(ImageExtractorConstants.DetectedLanguage.CODE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.DetectedLanguage.CODE_FIELD_NAME, language.getLanguageCode());
    }
    if (schema.getField(ImageExtractorConstants.DetectedLanguage.CONFIDENCE_FIELD_NAME) != null) {
      builder.set(ImageExtractorConstants.DetectedLanguage.CONFIDENCE_FIELD_NAME, language.getConfidence());
    }

    return builder.build();
  }

  /**
   * Retrieves Handwriting Annotation's non-nullable component schema. Handwriting Annotation's schema is retrieved
   * instead of using constant schema since users are free to choose to not include some of the fields.
   *
   * @return Handwriting Annotation's non-nullable component schema.
   */
  private Schema getHandwritingAnnotationSchema() {
    Schema handwritingAnnotationsFieldSchema = schema.getField(outputFieldName).getSchema();
    return handwritingAnnotationsFieldSchema.isNullable() ? handwritingAnnotationsFieldSchema.getNonNullable()
      : handwritingAnnotationsFieldSchema;
  }
}
