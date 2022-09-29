package com.manual.process.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aspose.pdf.License;
import com.aspose.pdf.facades.PdfFileEditor;
import com.manual.process.service.ManualProcessService;

@Service
public class ManualProcessServiceImpl implements ManualProcessService {
	
	public static final Logger logger = LoggerFactory.getLogger(ManualProcessServiceImpl.class);

	@Value("${pcl-evaluation-copies}")
	private boolean pclEvaluationCopies;

	@Value("${license-file-name}")
	private String licenseFileName;

	@Value("${input.file.location}")
	private String inputFileLocation;

	@Value("${process.file.location}")
	private String processFileLocation;

	@Value("${license.file.location}")
	private String licenseFileLocation;

	static List<String> fileList = new LinkedList<>();

	@Override
	public void manualPclCreationProcess() {
		final File folder = new File(inputFileLocation);
		listFilesForFolder(folder);
	}

	public void listFilesForFolder(final File folder) {
		try {
			List<String> fileList = new ArrayList<>();
			MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupMainMemoryOnly();
			String currentDateTimeStamp = currentDateTimeStamp();
			String currentDate = currentDate();
			PDFMergerUtility pdfMerger = new PDFMergerUtility();
			String mergePdfFile = "merge" + "_" + currentDateTimeStamp + ".pdf";
			fileList.add(licenseFileName);
			for (final File fileEntry : folder.listFiles()) {
				fileList.add(fileEntry.getName());
				if (fileEntry.isDirectory()) {
					listFilesForFolder(fileEntry);
				} else {
					logger.info("process file is "+fileEntry.getName());
					File inputFile = new File(fileEntry.getName());
					if (!(inputFile.exists())) {
						Files.move(Paths.get(inputFileLocation + fileEntry.getName()), Paths.get(fileEntry.getName()));
					}
					File copyFileLocation = new File(
							processFileLocation + currentDateTimeStamp + "-print" + "/" + fileEntry.getName());
					copyFileToTargetDirectory(inputFile, copyFileLocation);
					pdfMerger.addSource(fileEntry.getName());
				}
			}
			pdfMerger.setDestinationFileName(mergePdfFile);
			pdfMerger.mergeDocuments(memoryUsageSetting);
			fileList.add(mergePdfFile);
			File sourceFile = new File(mergePdfFile);
			File destFile = new File(processFileLocation + currentDate + "/" + mergePdfFile);
			copyFileToTargetDirectory(sourceFile, destFile);

			String status = convertPDFToPCL(mergePdfFile, currentDate);
			deleteFiles(fileList);
			logger.info(status);
		} catch (FileNotFoundException fileNotFoundException) {
			deleteFiles(fileList);
			logger.info("file is not found for processing:" + fileNotFoundException.getMessage());
		} catch (Exception exception) {
			deleteFiles(fileList);
			logger.info("Exception:" + exception.getMessage());
		}
	}

	public String currentDateTimeStamp() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		return dateFormat.format(date);
	}

	public String currentDate() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(date);
	}

	public void copyFileToTargetDirectory(File sourceFile, File destFile) {
		try {
			FileUtils.copyFile(sourceFile, destFile);
		} catch (IOException ioException) {
			logger.info("IOException:" + ioException.getMessage());
		} catch (Exception exception) {
			logger.info("Exception:" + exception.getMessage());
		}
	}

	public String convertPDFToPCL(String mergePdfFile, String currentDate) throws IOException {
		String outputPclFile = FilenameUtils.removeExtension(mergePdfFile) + ".pcl";
		String statusMessage = "process successfully compeleted";
		try {
			File licenseFileLoc = new File(licenseFileLocation + licenseFileName);
			File licenseFile = new File(licenseFileName);
			if (!(licenseFile.exists())) {
				Files.copy(licenseFileLoc.toPath(), licenseFile.toPath());
			}
			License license = new License();
			license.setLicense(licenseFileName);
			statusMessage = pclFileCreation(mergePdfFile, outputPclFile, currentDate);
		} catch (Exception exception) {
			statusMessage = "error with " + exception.getMessage();
		}
		if (pclEvaluationCopies) {
			statusMessage = "The license has expired:print pcl file with evaluation copies";
			pclFileCreation(mergePdfFile, outputPclFile, currentDate);
		}
		new File(outputPclFile).delete();
		return statusMessage;
	}

	public String pclFileCreation(String mergePdfFile, String outputPclFile, String currentDate) {
		String statusMessage = "process successfully compeleted";
		try {
			PdfFileEditor fileEditor = new PdfFileEditor();
			final InputStream stream = new FileInputStream(mergePdfFile);
			final InputStream[] streamList = new InputStream[] { stream };
			final OutputStream outStream = new FileOutputStream(outputPclFile);
			fileEditor.concatenate(streamList, outStream);
			stream.close();
			outStream.close();
			fileEditor.setCloseConcatenatedStreams(true);

			File sourceFile = new File(outputPclFile);
			File destFile = new File(processFileLocation + currentDate + "/" + outputPclFile);
			copyFileToTargetDirectory(sourceFile, destFile);
			logger.info("generate pcl file is:" + outputPclFile);
		} catch (Exception exception) {
			statusMessage = "error in pcl generate";
			System.out.println("Exception pclFileCreation() " + exception.getMessage());
		}
		return statusMessage;
	}

	public void deleteFiles(List<String> fileNameList) {
		for (String fileName : fileNameList) {
			File file = new File(fileName);
			if (file.exists()) {
				file.delete();
			}
		}
	}
}
