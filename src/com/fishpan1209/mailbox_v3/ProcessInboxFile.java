package com.fishpan1209.mailbox_v3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.liaison.mailbox.conf.MailboxConfig;
import com.liaison.mailbox.constants.MailboxConstants;
import com.liaison.mailbox.dao.ManipulateLockFile;
import com.liaison.mailbox.hub.CallInvokeServices;
import com.liaison.mailbox.preprocessors.InvokeCustomService;
import com.liaison.mailbox.processor.file.utilities.CallFileUtilitiesMethods;
import com.liaison.mailbox.processor.file.utilities.ConvertToProcFile;
import com.liaison.mailbox.processor.file.utilities.ResolvePassThruReceiverOrgID;
import com.liaison.mailbox.processor.lock.utilities.CallLockUtilitiesMethods;
import com.liaison.mailbox.processor.pgp.utilities.CallPGPUtilitiesMethods;

public class ProcessInboxFile {	
	private boolean preProcessorExecutionComplete = false;
	
	//Create a utilities classes objects.
	private final CallFileUtilitiesMethods 	cfumObject 	= new CallFileUtilitiesMethods();
	private final CallPGPUtilitiesMethods  	cpumObject 	= new CallPGPUtilitiesMethods();
	private final CallLockUtilitiesMethods  clumObject	= new CallLockUtilitiesMethods();
	private final CallInvokeServices 		cisObject	= new CallInvokeServices();

	public final void  callInboxProcess(final Logger log, final String Owner, final String MailslotName, final String FullPathName, final long RecSize,
										final long DelimiterLength, final String StripRecDel, final String PreProcessor, final String FlowCtrlSz, 
										final String CharSet, final String MaxLocks, final String DefaultVANOrgID, final String UseFxEnvelope, 
										final String SaveOriginal, final String SaveOriginalPathName, final String Sorting, final String PGPEnabled, 
										final String Recipient, final String Extension, final String JobQueue, final String PGPServerMountPoint, 
										final String Options, final String ReceiverOrgID, final String ReceiverLocID, final String ReceiverDataType, 
										final String ReceiverPayloadType, final String LockPath, final ManipulateLockFile lookup, final String[] SIDList, 
										final String from, final String to, final String host, final String node, final MailboxConfig config) throws RuntimeException {
		inboxProcess(log, Owner, MailslotName, FullPathName, RecSize, DelimiterLength, StripRecDel, PreProcessor, FlowCtrlSz, 
					 CharSet, MaxLocks, DefaultVANOrgID, UseFxEnvelope,	SaveOriginal, SaveOriginalPathName, Sorting, PGPEnabled, 
					 Recipient, Extension, JobQueue, PGPServerMountPoint, Options, ReceiverOrgID, ReceiverLocID, ReceiverDataType, 
					 ReceiverPayloadType, LockPath, lookup, SIDList, from, to, host, node, config);
	}
	
	private final void inboxProcess(final Logger log, final String Owner, final String MailslotName, final String FullPathName, final long RecSize,
									final long DelimiterLength, final String StripRecDel, final String PreProcessor, final String FlowCtrlSz, 
									final String CharSet, String MaxLocks, final String DefaultVANOrgID, final String UseFxEnvelope, 
									final String SaveOriginal, final String SaveOriginalPathName, final String Sorting, final String PGPEnabled, 
									final String Recipient, final String Extension, final String JobQueue, final String PGPServerMountPoint, 
									final String Options, String ReceiverOrgID, String ReceiverLocID, String ReceiverDataType, 
									final String ReceiverPayloadType, final String LockPath, final ManipulateLockFile lookup, final String[] SIDList, 
									final String from, final String to, final String host, final String node, final MailboxConfig config) throws RuntimeException {
		//Get variable values	
		MaxLocks = (MaxLocks == null? config.getRequiredProperty("MAX_LOCKED_INBOX_FILES") : MaxLocks);
		final boolean debug = Boolean.parseBoolean(config.getRequiredProperty("INBOX_DEBUG"));
		final String opaqueHome = config.getRequiredProperty("OPAQUE_HOME");
		final String updateInterval = config.getRequiredProperty("INMAIL_UPDATE_INTERVAL");
		final int lockDelayTime = Integer.parseInt(config.getRequiredProperty("LOCK_DELAY_TIME"));
		final int FlowControlSize = Integer.parseInt(FlowCtrlSz);	
		
		final String ProcFolderPath 		= FullPathName + MailboxConstants.PATHSUFFIX_PROC;
		final String OriginalFolderPath 	= FullPathName + MailboxConstants.PATHSUFFIX_ORIG;
		final String ErrorFolderPath 		= FullPathName + MailboxConstants.PATHSUFFIX_ERROR;

		String procFileName = null;
		String errorCode = null;	
	
		try {
			log.info("BEGIN: ProcessInboxFile - " + Owner + " | " + MailslotName);			
			
			String checkSize = "N";
			
			// Get a list of files in the path
			if (debug) log.info("ProcessInboxFile: " + LockPath + "|" + updateInterval + "|" + MaxLocks + "|" + checkSize + "|" + Sorting + "|" + lockDelayTime + "|" + debug);
			List<String> inboxFileList = cfumObject.callGetFileList(log, Owner, MailslotName, FullPathName, updateInterval, MaxLocks, checkSize, Sorting, lockDelayTime, debug);
			
			//TODO: Delete DB lock on mailslot after fetching files to process
			try {
				lookup.deleteDBLockFiles(log, Owner, MailslotName, LockPath);
				log.info("ProcessInboxFile: Lock released for " + LockPath);
			} catch (Exception e) {
				log.error("ProcessInboxFile: " + LockPath + " lock could not be released successfully. See nohup.out for Stacktrace");
				e.printStackTrace();
			}
			
			// Process if fileList is not empty
			if (inboxFileList == null || inboxFileList.isEmpty() || inboxFileList.size() == 0) {
				//Do nothing
				if (debug) log.info("ProcessInboxFile: No files to process in " + FullPathName + " ... checking ftp server in a separate thread...");
			} else {
				for (String inboxFileName : inboxFileList) {
					//Check if file is valid or not. If valid, process, if invalid, move to error folder
					boolean validFile = cfumObject.callFileExists(log, FullPathName, inboxFileName);
					String inboxFilePath = FullPathName + "/" + inboxFileName;
	
					log.info("ProcessInboxFile: Processing file - " + inboxFilePath);
					
					if (validFile) {
						// Process inbox file									
						// If Preprocess is not null, run preprocessor
						if (PreProcessor != null) {	
							log.info("ProcessInboxFile: PreProcessor: " + PreProcessor + " is being invoked on " + inboxFilePath);
							String serviceName = null;
							if (PreProcessor.equals("/data1/edimailbox/bin/storeSPLIT.sh")) {
								serviceName = "Custom.StoreSplit";
							} else { 
								final int indexOfDot = PreProcessor.indexOf(".") + 1;
								final String foundPackage = PreProcessor.substring(indexOfDot);
								serviceName = foundPackage.replace(":", ".");
							}
							
							if (debug) log.info(inboxFilePath + " : service being passed is ... " + serviceName);

							try {
								String preProcessorException = new InvokeCustomService().callInvokeCustomService(log, FullPathName, inboxFileName, serviceName, Owner, DefaultVANOrgID, to, from, host, config, debug);
								
								if (preProcessorException != null) {
									log.error("ProcessInboxFile: " + PreProcessor + " ran into exception on " + inboxFilePath + " \nException: " + preProcessorException);
								}
								
								boolean fileExists = cfumObject.callFileExists(log, FullPathName, inboxFileName);
								
								if (fileExists && preProcessorException == null)
									preProcessorExecutionComplete = true;
								else
									preProcessorExecutionComplete = false;
							} catch (Exception ex) {
								log.error("ProcessInboxFile: Failed to complete PreProcessor execution successfully: " + PreProcessor + " on file " + inboxFileName);
								cfumObject.callRenameAFile(log, FullPathName, inboxFileName, ErrorFolderPath, inboxFileName);
							}
						} else {
							preProcessorExecutionComplete = true;
						}	
						
						if (debug) log.info("ProcessInboxFile: PreProcessor execution complete on: " + inboxFileName + " ? " + preProcessorExecutionComplete);
						
						if (preProcessorExecutionComplete) {
							//Check if the file to be delivered requires FxEnvelope to be added.
							boolean needFxEnvelope = cfumObject.callRequireSeparateFxEnvelope(log, UseFxEnvelope, FullPathName, inboxFileName);
							
							if (needFxEnvelope) {
								log.info("ProcessInboxFile: ProcessInboxFile : Separating FxEnvelope and Payload from input file " + inboxFileName);
								//Copy file to original folder
								cfumObject.callCopyFile(log, FullPathName, SaveOriginalPathName, inboxFileName, inboxFileName+"-orig", SaveOriginal, "N", "N", "N", debug);
								cfumObject.callSeparateFxEnvelopeToFile(log, FullPathName, inboxFileName, debug);
							}
							
							final boolean needPGPDecryption = cpumObject.callRequirePGPDecryption(log, inboxFileName, PGPEnabled, Extension, Recipient, debug);
							final boolean needPGPEncryption = cpumObject.callRequirePGPEncryption(log, inboxFileName, PGPEnabled, Extension, Recipient, debug);
							boolean needFlowControl = false;
							
							if (needPGPDecryption) {
								if (SaveOriginal.equalsIgnoreCase("Y"))
									cfumObject.callCopyFile(log, FullPathName, SaveOriginalPathName, inboxFileName, inboxFileName, SaveOriginal, "N", "N", "N", debug);
								
								cpumObject.callDecryptJob(log, JobQueue, Owner, MailslotName, FullPathName, inboxFileName, PGPServerMountPoint);
							} else if (needPGPEncryption){
								if (SaveOriginal.equalsIgnoreCase("Y"))
									cfumObject.callCopyFile(log, FullPathName, SaveOriginalPathName, inboxFileName, inboxFileName, SaveOriginal, "N", "N", "N", debug);
								
								cpumObject.callEncryptJob(log, JobQueue, Owner, MailslotName, FullPathName, inboxFileName, Recipient, Options, Extension, PGPServerMountPoint);
							} else {
								log.info("ProcessInboxFile: ProcessInboxFile: " + inboxFilePath + ", Flow Control Flag: " + FlowControlSize);
								
								if (FlowControlSize != -1) {
									log.info("ProcessInboxFile: Checking whether flow control flag is turned on.");								
									needFlowControl = cfumObject.callCheckFlowControlFlag(log, FlowControlSize, ProcFolderPath, debug);								
								}
							}
							
							if (debug) log.info(inboxFileName + " ... charSet = " + CharSet);
							
							if (needFlowControl) {
								clumObject.callReleaseFileLock(FullPathName, inboxFileName);
							} else {							
								if (CharSet != null && CharSet.equalsIgnoreCase(MailboxConstants.ENCODING_UTF8)) {
									// UTF-8 branch								
									StringBuilder Payload = cfumObject.callReadFile(log, inboxFilePath, CharSet);
									
									if (Payload != null) {
										String beginsWith = Payload.substring(0,2);
										
										if (beginsWith.equals("ISA")) {
											//EDI files have to go through normal processing for now.
											ConvertToProcFile ctpfResult = cfumObject.callConvertToNormalProcFile(log, FullPathName, 
																									  ProcFolderPath, 
																									  inboxFileName, 
																									  inboxFileName, 
																									  SaveOriginal,  
																									  StripRecDel, 
																									  RecSize, 
																									  DelimiterLength, 
																									  debug);
											procFileName = ctpfResult.getProcFileName();

										} else {
											ResolvePassThruReceiverOrgID receiverOrgIDResults = cfumObject.callRetrieveReceiverOrg(ReceiverOrgID, ReceiverLocID, ReceiverDataType, inboxFilePath);
											
											ReceiverOrgID = receiverOrgIDResults.getToOrgID();
											ReceiverLocID = receiverOrgIDResults.getToLocID();
											ReceiverDataType = receiverOrgIDResults.getToDataType();
											String errorMessage = receiverOrgIDResults.getErrorMessage();
											
											if (errorMessage != null) {
												log.error("Error retrieving ReceiverOrgID, ReceiverLocID, ReceiverDataType ... " + errorMessage);
												errorCode = "ER";											
											} else {
												//Everything is in order, send data to URL Publish service
												errorCode = null;
												Map<String, String> urlParams = new HashMap<String, String>();
												
												String subject = "Opaque";
												
												urlParams.put("body", Payload.toString());
												urlParams.put("subject", subject);
												urlParams.put("payloadType", ReceiverPayloadType);
												urlParams.put("fromOrgID", Owner);
												urlParams.put("fromMsgID", inboxFileName);
												urlParams.put("toOrgID", ReceiverOrgID);
												urlParams.put("toLocID", ReceiverLocID);
												urlParams.put("dataType", ReceiverDataType);
												urlParams.put("encoding", CharSet);
												
												//Call URL Publish service
												cisObject.callURLPublish(log, urlParams, SIDList);											
											}
										}
									}								
								} else if (CharSet != null && CharSet.equalsIgnoreCase("EBCDIC")) {
									//EBCDIC branch, convert to ASCII before moving file to proc folder
									String tempOutFileName = "." + inboxFileName + "-ascii";
									String localException = cfumObject.callConvertEBCDICToASCII(log, FullPathName, inboxFileName, ProcFolderPath, 
																								tempOutFileName, debug);
									
									if (localException != null) {
										log.error("ProcessInboxFile: Exception encountered while trying to convert EBCDIC file to ASCII file: " + localException);
										//Do not continue processing
									} else {
										
										String opaqueDir = cfumObject.callCreateOpaqueDirectory(log, opaqueHome);
										
										//Copy original EBCDIC file to opaque folder as backup
										String localErrorMessage = cfumObject.callCopyFile(log, FullPathName, opaqueDir, inboxFileName, 
																						   inboxFileName + "-ebcdic", SaveOriginal, "N", 
																						   "N", "N", debug);
										if (localErrorMessage != null){
											//Stop, go no further in processing the file
										} else {
											// Original file is saved, no need to save another copy to original folder
											ConvertToProcFile ctpfResult = cfumObject.callConvertToNormalProcFile(log, ProcFolderPath, 
																							  ProcFolderPath, 
																							  tempOutFileName, 
																							  inboxFileName, 
																							  "N",  
																							  StripRecDel, 
																							  RecSize, 
																							  DelimiterLength, 
																							  debug);
											procFileName = ctpfResult.getProcFileName();
										}
									}								
								} else {
									//Default branch, Charset was neither UTF-8 nor EBCDIC
									ConvertToProcFile ctpfResult = cfumObject.callConvertToNormalProcFile(log, FullPathName, 
																						  ProcFolderPath, 
																						  inboxFileName, 
																						  inboxFileName, 
																						  SaveOriginal,  
																						  StripRecDel, 
																						  RecSize, 
																						  DelimiterLength, 
																						  debug);
									procFileName = ctpfResult.getProcFileName();
								}
								
								if (errorCode == null) {
									if (procFileName == null) {
										log.info("ProcessInboxFile: ProcFileName is null, file was probably sent to URL adapter" + inboxFilePath);
									}
								
									if (SaveOriginal.equalsIgnoreCase("Y")) {
										//Save file to original folder
										try {
											cfumObject.callRenameAFile(log, FullPathName, inboxFileName, OriginalFolderPath, procFileName);
										} catch (Exception e) {
											log.error("ProcessInboxFile: Exception encountered while saving inbox file to original folder: " + e.getMessage());
										}
									}							
								} else if (errorCode.equalsIgnoreCase("ER")) {
									//Release lock files
									clumObject.callReleaseFileLock(FullPathName, inboxFileName);
								} else {
									log.info("ProcessInboxFile: ErrorCode was not 'ER'. Trace me to figure out the reason..." + inboxFilePath);								
								}							
							}						
							log.info("ProcessInboxFile: Complete processing file - " + inboxFilePath);
						} else {
							//Do not process file
							log.info("ProcessInboxFile: Complete processing file - " + inboxFilePath);
						}
					} else {
						//Log exception and move file to error folder
						log.error("ERROR: ProcessInboxFile - " + inboxFilePath);
						try {
							cfumObject.callRenameAFile(log, FullPathName, inboxFileName, ErrorFolderPath, inboxFileName);
						} catch (Exception e) {
							log.debug("ProcessInboxFile: Exception encountered while trying to move inbox file to error folder: " + e.getMessage());
						}
					}
				} //end for loop
			}			
			log.info("ProcessInboxFile: END: ProcessInboxFile - " + Owner + " | " + MailslotName);			
		} catch (Exception ex) {
			log.info("ERROR: ProcessInboxFile - " + Owner + " | " + MailslotName);
			log.error("ProcessInboxFile: Exception encountered while processing file in " + FullPathName + " ... " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
