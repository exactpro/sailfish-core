/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf.common.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvReader;


public class Utils
{
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	private static final boolean isDebugged = logger.isDebugEnabled();

	public enum CompType
	{
		TIME,
		NAME,
		END_NAME,
		TIME_END_NAME;
	}

	public static long determineStartSecFromParsedFile(String parsedFileName) throws IOException
	{
		CsvReader parsedReader = new CsvReader(parsedFileName);

		parsedReader.setSafetySwitch(false);

		parsedReader.readHeaders();

		parsedReader.readRecord();

		long startSec = Long.parseLong(parsedReader.getValues()[0]);

		parsedReader.close();

		return startSec;
	}


	public static class FileSpecialComparator implements Comparator<File>
	{
		private boolean first;
		private boolean excludeExt;

		public FileSpecialComparator(boolean first, boolean excludeExt)
		{
			this.first = first;
			this.excludeExt = excludeExt;
		}


		@Override
		public int compare(File o1, File o2)
		{
			int end_index = 0;
			int start_index = 0;

			if ( first )
			{
				if ( !excludeExt )
				{
					end_index = o1.getName().lastIndexOf("_");
					start_index = o1.getName().lastIndexOf("_", end_index - 1);
				}
				else
				{
					end_index = getFileNameWithoutExt(o1.getName()).lastIndexOf("_");
					start_index = getFileNameWithoutExt(o1.getName()).lastIndexOf("_", end_index - 1);
				}

			}
			else
			{
				if ( !excludeExt )
				{
					start_index = o1.getName().lastIndexOf("_");
					end_index = o1.getName().length();
				}
				else
				{
					start_index = getFileNameWithoutExt(o1.getName()).lastIndexOf("_");
					end_index = getFileNameWithoutExt(o1.getName()).length();
				}
			}

			long l1 = Long.parseLong(o1.getName().substring(start_index + 1, end_index));

			if ( first )
			{
				if ( !excludeExt )
				{
					end_index = o2.getName().lastIndexOf("_");
					start_index = o2.getName().lastIndexOf("_", end_index - 1);
				}
				else
				{
					end_index = getFileNameWithoutExt(o2.getName()).lastIndexOf("_");
					start_index = getFileNameWithoutExt(o2.getName()).lastIndexOf("_", end_index - 1);
				}

			}
			else
			{
				if ( !excludeExt )
				{
					start_index = o2.getName().lastIndexOf("_");
					end_index = o2.getName().length();
				}
				else
				{
					start_index = getFileNameWithoutExt(o2.getName()).lastIndexOf("_");
					end_index = getFileNameWithoutExt(o2.getName()).length();
				}
			}

			long l2 = Long.parseLong(o2.getName().substring(start_index + 1, end_index));

			if ( l1 == l2 )
				return 0;

			if ( l1 > l2 )
				return 1;
			else
				return -1;
		}
	}



	public static class FileComparator implements Comparator<File>
	{
		private CompType type;
		private String prefix;
		private boolean extension;

		public FileComparator(CompType type, String prefix, boolean extension)
		{
			this.type = type;
			this.prefix = prefix;
			this.extension = extension;
		}


		@Override
		public int compare(File o1, File o2)
		{
			if ( type == CompType.TIME )
			{
				if ( o1.lastModified() < o2.lastModified() )
					return -1;
				else if ( o1.lastModified() > o2.lastModified() )
					return 1;

				return 0;
			}
			else if ( type == CompType.NAME )
			{
				return o1.getName().compareTo(o2.getName());
			}
			else if ( type == CompType.END_NAME )
			{
				String os1 = null;

				if ( o1.getName().length() == this.prefix.length() )
					os1 = "0";
				else
				{
					if ( !this.extension )
						os1 = o1.getName().substring(this.prefix.length());
					else
						os1 = getFileNameWithoutExt(o1.getName()).substring(this.prefix.length());
				}

				String os2 = null;

				if ( o2.getName().length() == this.prefix.length() )
					os2 = "0";
				else
				{
					if ( !this.extension )
						os2 = o2.getName().substring(this.prefix.length());
					else
						os2 = getFileNameWithoutExt(o2.getName()).substring(this.prefix.length());
				}

				return Integer.parseInt(os1) - Integer.parseInt(os2);
			}
			else if ( type == CompType.TIME_END_NAME )
			{
				if ( o1.lastModified() < o2.lastModified() )
					return -1;
				else if ( o1.lastModified() > o2.lastModified() )
					return 1;

				String os1 = null;

				if ( o1.getName().length() == this.prefix.length() )
					os1 = "0";
				else
				{
					if ( !this.extension )
						os1 = o1.getName().substring(this.prefix.length());
					else
						os1 = getFileNameWithoutExt(o1.getName()).substring(this.prefix.length());
				}

				String os2 = null;

				if ( o2.getName().length() == this.prefix.length() )
					os2 = "0";
				else
				{
					if ( !this.extension )
						os2 = o2.getName().substring(this.prefix.length());
					else
						os2 = getFileNameWithoutExt(o2.getName()).substring(this.prefix.length());
				}

				return Integer.parseInt(os1) - Integer.parseInt(os2);

			}
			else throw new IllegalArgumentException("Unknown CompType = [" + type + "]");
		}
	}

	public static class FileExtensionFilter implements FileFilter{

		private String ext;

		public FileExtensionFilter(String ext){
			this.ext = ext;
		}

		@Override
		public boolean accept(File file) {
			return file.getName().endsWith("."+ext);
		}

	}

	public static class FileNameFilter implements FileFilter
	{
		private final String fileNamePrefix;


		public FileNameFilter(final String fileNamePrefix)
		{
			this.fileNamePrefix = fileNamePrefix;
		}


		@Override
		public boolean accept(File file)
		{
			if ( file.getName().startsWith(this.fileNamePrefix) )
				return true;

			return false;
		}
	}




	public static String getFileNameWithoutExt(String fileName)
	{
		int pos = fileName.lastIndexOf(".");

		if ( pos != -1 )
			fileName = fileName.substring(0, pos);

		return fileName;
	}


	public static long retrieveStartTimeForCalculating(File latencyFolder, File inputFolder, File outputFolder, long maxLatencyWindow)
	{
		File[] files = latencyFolder.listFiles();

		Arrays.sort(files, new Utils.FileSpecialComparator(true, false));

		if ( files.length > 0 )
		{
			int i = 0;

			boolean calculate = false;

			long startInterval = 0;

			int startFileIndex = 0;

			File lastLatFile = files[files.length - 1];

			File[] inputFiles = inputFolder.listFiles();

			Arrays.sort(inputFiles, new Utils.FileSpecialComparator(true, false));

			i = 0;

			for ( ; i < inputFiles.length; ++i )
			{
				if ( FileUtils.isFileOlder(inputFiles[inputFiles.length - 1 - i], lastLatFile) )
					break;
			}

			startFileIndex = inputFiles.length - i;

			if ( startFileIndex < inputFiles.length )
			{
				calculate = true;

				long startTimeStamp = determineStartInterval(inputFiles[startFileIndex].getName());

				startInterval = startTimeStamp / (maxLatencyWindow*1000000);
			}

			File[] outputFiles = outputFolder.listFiles();

			Arrays.sort(outputFiles, new Utils.FileSpecialComparator(true, false));

			i = 0;

			for ( ; i < outputFiles.length; ++i )
			{
				if ( FileUtils.isFileOlder(outputFiles[outputFiles.length - 1 - i], lastLatFile) )
					break;
			}

			startFileIndex = outputFiles.length - i;

			if ( startFileIndex < outputFiles.length )
			{
				long startTimeStamp = determineStartInterval(outputFiles[startFileIndex].getName());

				long tempStartInterval = startTimeStamp / (maxLatencyWindow*1000000);

				if ( calculate )
				{
					if ( startInterval > tempStartInterval )
						startInterval = tempStartInterval;
				}
				else
				{
					startInterval = tempStartInterval;
					calculate = true;
				}
			}

			if ( calculate )
				return maxLatencyWindow*startInterval;
			else
				return -1;
		}

		return 0;
	}



	public static long retrieveStartTimeForCalculating(String latencyFolderName, long maxLatencyWindow)
	{
		File latencyFolder = new File(latencyFolderName);

		if ( latencyFolder.exists() )
		{
			File[] files = latencyFolder.listFiles();

			Arrays.sort(files, new Utils.FileComparator(CompType.TIME, "", false));

			if ( files.length == 0 || files.length == 1 )
				return 0;

			return maxLatencyWindow*(files.length - 2);
		}
		else
			throw new EPSCommonException("Could not find [" + latencyFolderName + "] folder with latency files");
	}


	public static void generateInputFile(String folderName, long startTimeInSec, long finishTimeInSec, String generatingFile) throws IOException
	{
		File folder = new File(folderName);

		if ( folder.exists() )
		{
			File[] files = folder.listFiles();

			Arrays.sort(files, new Utils.FileSpecialComparator(true, false));

			if ( files.length == 0 )
				return;

			int index = ( files.length - 1 );
			for ( ; index >= 0; --index )
			{
				long startInterval = determineStartInterval(files[index].getName());

				if ( startInterval < startTimeInSec*1000000 )
					break;
			}

			if ( index < 0 )
				index = 0;



			BufferedWriter writer = new BufferedWriter(new FileWriter(generatingFile));

			for ( int i = index; i < files.length; ++i )
			{
				if ( isDebugged )
				{
					logger.debug(files[i].getName());
				}

				CsvReader reader = new CsvReader(folderName + File.separator + files[i].getName());
				reader.setSafetySwitch(false);

				reader.readRecord();

				if ( i == index )
				{
					writer.write(reader.getRawRecord());
					writer.newLine();
				}

				while ( reader.readRecord() )
				{
					long timestamp = Long.parseLong(reader.getValues()[0]);

					if ( timestamp >= startTimeInSec*1000000 && ( timestamp < finishTimeInSec*1000000 || finishTimeInSec == Long.MAX_VALUE ))
					{
						writer.write(reader.getRawRecord());
						writer.newLine();
					}
					else if ( timestamp < startTimeInSec*1000000 )
					{
					}
					else
						break;

				}

				reader.close();
			}

			writer.close();

		}
		else
			throw new EPSCommonException("Could not find [" + folderName + "] folder with files");
	}


	public static void generateMDLatFile(String folderName, long startTimeInSec, long finishTimeInSec, String generatingFile, SimpleDateFormat format, String analyzedField, long startSec) throws IOException
	{
		File folder = new File(folderName);

		if ( folder.exists() )
		{
			File[] files = folder.listFiles();

			Arrays.sort(files, new Utils.FileSpecialComparator(true, false));

			if ( files.length == 0 )
				return;

			int index = ( files.length - 1 );
			for ( ; index >= 0; --index )
			{
				long startInterval = determineStartInterval(files[index].getName());

				if ( startInterval < startTimeInSec*1000000 )
					break;
			}

			if ( index < 0 )
				index = 0;


            try(BufferedWriter writer = new BufferedWriter(new FileWriter(generatingFile))) {
                int fieldIndex = -1;

                for(int i = index; i < files.length; ++i) {
                    if(isDebugged) {
                        logger.debug(files[i].getName());
                    }

                    CsvReader reader = new CsvReader(folderName + File.separator + files[i].getName());
                    reader.setSafetySwitch(false);

                    reader.readRecord();

                    if(i == index) {

                        String[] headers = reader.getValues();

                        for(int j = 0; j < headers.length; ++j) {
                            if(headers[j].equals(analyzedField)) {
                                fieldIndex = j;
                                break;
                            }
                        }

                        writer.write("Timestamp,Latency");

                        for(int j = 3; j < headers.length; ++j)
                            writer.write("," + headers[j]);

                        writer.newLine();

                        if(fieldIndex == -1)
                            throw new IllegalArgumentException("Could not find fieldName = [" + analyzedField + "] in inputFile = [" + files[i].getName() + "]");
                    }

                    while(reader.readRecord()) {
                        String[] values = reader.getValues();

                        long timestamp = Long.parseLong(values[0]);

                        if(timestamp >= startTimeInSec * 1000000 && (timestamp < finishTimeInSec * 1000000 || finishTimeInSec == Long.MAX_VALUE)) {
                            try {
                                String sendingTime = values[fieldIndex];

                                long sendingTimestamp = format.parse(sendingTime + " +0000").getTime();

                                sendingTimestamp = (sendingTimestamp - startSec * 1000) * 1000;

                                writer.write(values[0] + "," + Long.toString(timestamp - sendingTimestamp));

                                for(int j = 3; j < values.length; ++j)
                                    writer.write("," + values[j]);

                                writer.newLine();
                            } catch(ParseException e) {
                            }

                        } else if(timestamp < startTimeInSec * 1000000) {
                        } else
                            break;

                    }

                    reader.close();
                }
			}
		}
		else
			throw new EPSCommonException("Could not find [" + folderName + "] folder with files");
	}







	public static void generateFakeInputFile(String folderName, long startTimeInSec, long finishTimeInSec, String generatingFile) throws IOException
	{
		File folder = new File(folderName);

		if ( folder.exists() )
		{
			File[] files = folder.listFiles();

			Arrays.sort(files, new Utils.FileSpecialComparator(true, false));

			if ( files.length == 0 )
				return;

			int index = ( files.length - 1 );
			for ( ; index >= 0; --index )
			{
				long startInterval = determineStartInterval(files[index].getName());

				if ( startInterval < startTimeInSec*1000000 )
					break;
			}

			if ( index < 0 )
				index = 0;

			BufferedWriter writer = new BufferedWriter(new FileWriter(generatingFile));


			boolean firstRecordWritten = false;

			for ( int i = index; i < files.length; ++i )
			{
				if ( isDebugged )
				{
					logger.debug(files[i].getName());
				}

				CsvReader reader = new CsvReader(folderName + File.separator + files[i].getName());
				reader.setSafetySwitch(false);

				reader.readRecord();

				if ( i == index )
				{
					writer.write(reader.getRawRecord());
					writer.newLine();
				}


				if ( !firstRecordWritten || i == files.length - 1)
				{
					String lastRecord = "";

					while ( reader.readRecord() )
					{
						long timestamp = Long.parseLong(reader.getValues()[0]);

						lastRecord = reader.getRawRecord();

						if ( !firstRecordWritten )
						{
							if ( timestamp >= startTimeInSec*1000000 && ( timestamp < finishTimeInSec*1000000 || finishTimeInSec == Long.MAX_VALUE ))
							{
								writer.write(reader.getRawRecord());
								writer.newLine();
								firstRecordWritten = true;
							}
							else if ( timestamp < startTimeInSec*1000000 )
							{
							}
							else
								break;
						}
					}

					writer.write(lastRecord);
				}

				reader.close();
			}

			writer.close();

		}
		else
			throw new EPSCommonException("Could not find [" + folderName + "] folder with files");
	}



















	public static long determineFinishInterval(String fileName)
	{
		long finishTime = Long.parseLong(Utils.getFileNameWithoutExt(fileName).substring(Utils.getFileNameWithoutExt(fileName).lastIndexOf("_") + 1));

		return finishTime;
	}


	public static long determineStartInterval(String fileName)
	{
		int lastIndex = fileName.lastIndexOf("_");

		if ( lastIndex == -1 )
			throw new EPSCommonException("Incorrect file name = [" + fileName + "]");

		int prevIndex = fileName.lastIndexOf("_", lastIndex - 1);

		long startTime = Long.parseLong(fileName.substring(prevIndex + 1, lastIndex));

		return startTime;
	}


	public static void mergeFiles(String folderName, String generatingFile) throws IOException
	{
		File folder = new File(folderName);

		if ( folder.exists() )
		{
			File[] files = folder.listFiles();

			Arrays.sort(files, new Utils.FileComparator(CompType.TIME, "", false));

			if ( files.length == 0 )
				return;

			BufferedWriter writer = new BufferedWriter(new FileWriter(generatingFile));

			for ( int i = 0; i < files.length; ++i )
			{
				CsvReader reader = new CsvReader(folderName + File.separator + files[i].getName());
				reader.setSafetySwitch(false);

				reader.readRecord();

				if ( i == 0 )
				{
					writer.write(reader.getRawRecord());
					writer.newLine();
				}

				while ( reader.readRecord() )
				{
					writer.write(reader.getRawRecord());
					writer.newLine();
				}

				reader.close();
			}

			writer.close();

		}
		else
			throw new EPSCommonException("Could not find [" + folderName + "] folder with files");
	}


	public static boolean lockFile(File file) throws IOException
	{
		String lockFileName = "." + file.getName();

		File lockFile = new File(file.getParentFile(), lockFileName);

		return lockFile.createNewFile();
	}


	public static boolean unlockFile(File file) throws IOException
	{
		String lockFileName = "." + file.getName();

		File lockFile = new File(file.getParentFile(), lockFileName);

		return lockFile.delete();
	}


	public static void deleteFileWithoutException(String destFile)
	{
		try
		{
			FileUtils.forceDelete(new File(destFile));
		}
		catch ( Exception e )
		{

		}
	}


	public static void deleteFileWithoutException(File destFile)
	{
		try
		{
			FileUtils.forceDelete(destFile);
		}
		catch ( Exception e )
		{

		}
	}


}
