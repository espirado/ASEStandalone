/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.molgenis.genotype.bgen;

/**
 *
 * @author patri
 */
public class BgenixMetadata {
	
	private final String fileName;
	private final int fileSize;
	private final long lastWriteTime;
	private final byte[] first1000bytes;
	private final long indexCreationTime;

	public BgenixMetadata(String fileName, int fileSize, long lastWriteTime, byte[] first1000bytes, long indexCreationTime) {
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.lastWriteTime = lastWriteTime;
		this.first1000bytes = first1000bytes;
		this.indexCreationTime = indexCreationTime;
	}

	public String getFileName() {
		return fileName;
	}

	public int getFileSize() {
		return fileSize;
	}

	public long getLastWriteTime() {
		return lastWriteTime;
	}

	public byte[] getFirst1000bytes() {
		return first1000bytes;
	}

	public long getIndexCreationTime() {
		return indexCreationTime;
	}
	
}
