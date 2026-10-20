package com.htx.app.pdftomd.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.docx4j.convert.out.HTMLSettings;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.docx4j.Docx4J;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ConversionService {

	public String convert(MultipartFile file) throws Exception {
		String name = file.getOriginalFilename();
		if (name == null)
			throw new IllegalArgumentException("Nombre de archivo no válido.");
		String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();

		try (InputStream in = file.getInputStream()) {
			return switch (ext) {
			case "pdf" -> convertPdf(in);
			case "docx" -> convertDocx(in);
			case "xlsx", "xls" -> convertExcel(in);
			case "pptx" -> convertPptx(in);
			default ->
				throw new IllegalArgumentException("Formato no soportado: ." + ext + ". Use PDF, DOCX, XLSX o PPTX.");
			};
		}
	}

	// ---------- PDF ----------
	private String convertPdf(InputStream in) throws IOException {
		try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
			// temporal
		    System.out.println("Páginas: " + doc.getNumberOfPages());
		    PDPage p0 = doc.getPage(0);
		    System.out.println("Fuentes en página 1: " + p0.getResources().getFontNames());
		    System.out.println("XObjects en página 1: " + p0.getResources().getXObjectNames());

		    PDFTextStripper stripperr = new PDFTextStripper();
		    int conTexto = 0;
		    for (int p = 1; p <= doc.getNumberOfPages(); p++) {
		        stripperr.setStartPage(p);
		        stripperr.setEndPage(p);
		        String t = stripperr.getText(doc).trim();
		        if (!t.isEmpty()) conTexto++;
		    }
		    System.out.println("Páginas con texto extraíble: " + conTexto + "/" + doc.getNumberOfPages());
		    // temporal
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(doc);
			return "# Texto extraído del PDF\n\n" + text;
		}
	}

	// ---------- WORD (.docx) ----------
	@SuppressWarnings("deprecation")
	private String convertDocx(InputStream in) throws Exception {
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(in);

		// Usar la factoría de Docx4J, no "new HTMLSettings()"
		HTMLSettings htmlSettings = Docx4J.createHTMLSettings();
		htmlSettings.setWmlPackage(wordMLPackage);

		ByteArrayOutputStream htmlBytes = new ByteArrayOutputStream();

		// FLAG_EXPORT_PREFER_XSL es el modo estándar para HTML
		Docx4J.toHTML(htmlSettings, htmlBytes, Docx4J.FLAG_EXPORT_PREFER_XSL);

		String html = htmlBytes.toString(StandardCharsets.UTF_8.name());
		return FlexmarkHtmlConverter.builder().build().convert(html);
	}

	// ---------- EXCEL ----------
	private String convertExcel(InputStream in) throws IOException {
		StringBuilder md = new StringBuilder();
		try (Workbook wb = WorkbookFactory.create(in)) {
			DataFormatter formatter = new DataFormatter();
			for (int s = 0; s < wb.getNumberOfSheets(); s++) {
				Sheet sheet = wb.getSheetAt(s);
				md.append("## ").append(sheet.getSheetName()).append("\n\n");

				int lastRow = sheet.getLastRowNum();
				if (lastRow < 0) {
					md.append("_(hoja vacía)_\n\n");
					continue;
				}

				int maxCols = 0;
				for (int r = 0; r <= lastRow; r++) {
					Row row = sheet.getRow(r);
					if (row != null && row.getLastCellNum() > maxCols) {
						maxCols = row.getLastCellNum();
					}
				}
				if (maxCols == 0)
					continue;

				md.append("| ");
				for (int c = 0; c < maxCols; c++) {
					md.append("Col").append(c + 1).append(" | ");
				}
				md.append("\n|");
				for (int c = 0; c < maxCols; c++)
					md.append("---|");
				md.append("\n");

				for (int r = 0; r <= lastRow; r++) {
					Row row = sheet.getRow(r);
					md.append("| ");
					for (int c = 0; c < maxCols; c++) {
						Cell cell = (row != null) ? row.getCell(c) : null;
						String val = (cell != null) ? formatter.formatCellValue(cell) : "";
						md.append(val.replace("|", "\\|").replace("\n", " ")).append(" | ");
					}
					md.append("\n");
				}
				md.append("\n");
			}
		}
		return md.toString();
	}

	// ---------- POWERPOINT (.pptx) ----------
	private String convertPptx(InputStream in) throws IOException {
		StringBuilder md = new StringBuilder();
		try (XMLSlideShow ppt = new XMLSlideShow(in)) {
			List<XSLFSlide> slides = ppt.getSlides();
			for (int i = 0; i < slides.size(); i++) {
				XSLFSlide slide = slides.get(i);
				md.append("## Diapositiva ").append(i + 1).append("\n\n");

				for (XSLFShape shape : slide.getShapes()) {
					if (shape instanceof XSLFTextShape textShape) {
						String text = textShape.getText();
						if (text != null && !text.isBlank()) {
							md.append(text.strip()).append("\n\n");
						}
					}
				}
				md.append("---\n\n");
			}
		}
		return md.toString();
	}

}
