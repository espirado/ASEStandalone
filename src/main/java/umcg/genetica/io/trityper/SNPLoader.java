/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umcg.genetica.io.trityper;

// import jdk.internal.ref.Cleaner;
import sun.nio.ch.DirectBuffer;
import umcg.genetica.io.Gpio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


/**
 * @author harm-jan
 */
public class SNPLoader {

    private RandomAccessFile m_genotypehandle;
    private RandomAccessFile m_dosagehandle;

    private int m_numIndividuals;


    private final Boolean[] m_isIncluded, m_isFemale;

    private ByteBuffer mappedGenotypeHandle = null;
    private ByteBuffer mappedDosageHandle = null;

    private long currentGtMapStart;
    private long currentGtMapEnd;
    private long currentDosageMapStart;
    private long currentDosageMapEnd;
    private byte[] bDs;
    private byte[] bGt;

    public SNPLoader(RandomAccessFile genotypehandle, Boolean[] indIsIncluded, Boolean[] isFemale) {
        this(genotypehandle, null, indIsIncluded, isFemale, 1000);
    }

    public SNPLoader(RandomAccessFile genotypehandle, RandomAccessFile dosagehandle, Boolean[] indIsIncluded, Boolean[] isFemale) {
        this(genotypehandle, dosagehandle, indIsIncluded, isFemale, 1000);
    }

    int gtmaplen;
    int dsmaplen;
    int numberOfVariantsInMemoryMap = 1000;

    public SNPLoader(RandomAccessFile genotypeHandle, RandomAccessFile dosageHandle, Boolean[] isIncluded, Boolean[] isFemale, int numberOfVariantsToBuffer) {

        m_genotypehandle = genotypeHandle;
        m_dosagehandle = dosageHandle;
        m_isIncluded = isIncluded;
        m_isFemale = isFemale;
        this.numberOfVariantsInMemoryMap = numberOfVariantsToBuffer;
    }

    public void loadGenotypes(SNP snp) throws IOException {
        byte[] allele1 = new byte[m_numIndividuals];
        byte[] allele2 = new byte[m_numIndividuals];

        int nrBytesToRead = m_numIndividuals * 2;
        long seekLoc = (long) snp.getId() * (long) nrBytesToRead;

//		byte[] alleles = new byte[nrBytesToRead];

        // initiate buffer if it doesn't exist, or if location we're looking for is beyond the current map
        long seekEnd = seekLoc + (m_numIndividuals * 2);
//		System.out.println("Seekloc: " + seekLoc);
//		System.out.println("SeekEnd: " + seekEnd);
        if (mappedGenotypeHandle == null || seekLoc < currentGtMapStart || seekLoc > currentGtMapEnd || seekEnd > currentGtMapEnd) {
            // 32 megabytes worth of variants; (32*1048576)/(m_numIndividuals * 2) bytes
            if (mappedGenotypeHandle == null) {

                int bytesPerVariant = (m_numIndividuals * 2);
                int nrBytesPerBuffer = bytesPerVariant * numberOfVariantsInMemoryMap; //(32 * 1048576);
                while (nrBytesPerBuffer < 0) {
                    numberOfVariantsInMemoryMap /= 2;
                    nrBytesPerBuffer = bytesPerVariant * numberOfVariantsInMemoryMap; //(32 * 1048576);

//					System.out.println("WARNING: BUFFER OVERFLOW! Setting max number of variants in memory to " + numberOfVariantsInMemoryMap);
//					System.out.println("Buffer will be " + Gpio.humanizeFileSize(nrBytesPerBuffer) + " (" + nrBytesPerBuffer + "b)");
                }
//				int remainder = nrBytesPerBuffer % bytesPerVariant;
//				nrBytesPerBuffer += remainder;
                gtmaplen = nrBytesPerBuffer;
                dsmaplen = nrBytesPerBuffer / 2;
//				System.out.println("bytes in buffer1: " + gtmaplen);
//				System.out.println("bytes in buffer2: " + dsmaplen);
            }


            // prevent overflow
            int maplentouse = gtmaplen;
            if (seekLoc + maplentouse > m_genotypehandle.length()) {
                maplentouse = (int) (m_genotypehandle.length() - seekLoc);
            }

            FileChannel gtChannel = m_genotypehandle.getChannel();
            gtChannel.position(seekLoc);
            if (mappedGenotypeHandle == null || mappedGenotypeHandle.capacity() != maplentouse) {
                mappedGenotypeHandle = ByteBuffer.allocateDirect(maplentouse);
            } else {
                ((Buffer) mappedGenotypeHandle).clear();
            }

            gtChannel.read(mappedGenotypeHandle);

//            mappedGenotypeHandle = m_genotypehandle.getChannel().map(FileChannel.MapMode.READ_ONLY, seekLoc, maplentouse);
//            mappedGenotypeHandle.load();
//            bGt = new byte[(int) maplentouse];
//            mappedGenotypeHandle.get(bGt);
//            mappedGenotypeHandle.g
            ((Buffer) mappedGenotypeHandle).flip();
            currentGtMapStart = seekLoc;
            currentGtMapEnd = currentGtMapStart + maplentouse;
//            System.out.println("Reload buffer:\t" + seekLoc + "\tstart\t" + currentGtMapStart + "\tstop\t" + currentGtMapEnd + "\tlen\t" + maplentouse);

        }

//		if (m_genotypehandle.getFilePointer() != seekLoc) {
//			m_genotypehandle.seek(seekLoc);
//		}

//		m_genotypehandle.read(alleles, 0, bytesize);
//		mappedGenotypeHandle(alleles, 0, bytesize);

        // recalculate where we should be looking for this particular snp


//		mappedGenotypeHandle.get(alleles, offset, nrBytesToRead);
//		mappedGenotypeHandle.slice();

        int offset = (int) (seekLoc - currentGtMapStart);
//        System.out.println("Seekloc: " + seekLoc);
//        System.out.println("offset: " + offset);
//        System.out.println("btr: " + nrBytesToRead);
//        System.out.println("capacity: " + mappedGenotypeHandle.capacity());
//        System.out.println("remaining: " + mappedGenotypeHandle.remaining());
//        System.out.println("limit:" + mappedGenotypeHandle.limit());
//        System.out.println("position:" + mappedGenotypeHandle.position());
//        System.out.println("req: " + m_numIndividuals);

        ((Buffer) mappedGenotypeHandle).position(offset);

        mappedGenotypeHandle.get(allele1, 0, m_numIndividuals);
        mappedGenotypeHandle.get(allele2, 0, m_numIndividuals);
//        System.arraycopy(bGt, offset, allele1, 0, m_numIndividuals);
//        System.arraycopy(bGt, offset + m_numIndividuals, allele2, 0, m_numIndividuals);
//		System.arraycopy(alleles, 0, allele1, 0, m_numIndividuals);
//		System.arraycopy(alleles, m_numIndividuals, allele2, 0, m_numIndividuals);

//		alleles = null;

        snp.setAlleles(allele1, allele2, m_isIncluded, m_isFemale);

    }

    public void loadDosage(SNP snp) throws IOException {
        if (m_dosagehandle != null) {
            byte[] dosageValues = new byte[m_numIndividuals];
            //if (loadedSNP.getGcScores()==null||loadedSNP.getThetaValues()==null||loadedSNP.getRValues()==null) {
            long seekLoc = (long) snp.getId() * (long) m_numIndividuals;

            // initiate buffer if it doesn't exist, or if location we're looking for is beyond the current map
            long seekEnd = seekLoc + (m_numIndividuals);
            if (mappedDosageHandle == null || seekLoc < currentDosageMapStart || seekLoc > currentDosageMapEnd || seekEnd > currentDosageMapEnd) {
                int maplentouse = dsmaplen;
                // prevent overflow
                if (seekLoc + maplentouse > m_dosagehandle.length()) {
                    maplentouse = (int) (m_dosagehandle.length() - seekLoc);
                }

                FileChannel dosageCh = m_dosagehandle.getChannel();
                dosageCh.position(seekLoc);


                if (mappedDosageHandle == null || mappedDosageHandle.capacity() != maplentouse) {
                    mappedDosageHandle = ByteBuffer.allocateDirect(maplentouse);
                } else {
                    ((Buffer) mappedDosageHandle).clear();
                }


//                mappedDosageHandle = m_dosagehandle.getChannel().map(FileChannel.MapMode.READ_ONLY, seekLoc, maplentouse);
//                mappedDosageHandle.load();
//                bDs = new byte[(int) maplentouse];
//                mappedDosageHandle.get(bDs);

                dosageCh.read(mappedDosageHandle);
                ((Buffer) mappedDosageHandle).flip();
                currentDosageMapStart = seekLoc;
                currentDosageMapEnd = currentDosageMapStart + maplentouse;
            }

//			m_dosagehandle.seek(seekLoc);
//			m_dosagehandle.read(dosageValues, 0, m_numIndividuals);
//
            int offset = (int) (seekLoc - currentDosageMapStart);
            ((Buffer) mappedDosageHandle).position(offset);
            mappedDosageHandle.get(dosageValues, 0, m_numIndividuals);
//            System.arraycopy(bDs, offset, dosageValues, 0, m_numIndividuals);


            byte[] genotypes = snp.getGenotypes();
            boolean takeComplement = false;
            for (int ind = 0; ind < dosageValues.length; ind++) {
                double dosagevalue = ((double) (-Byte.MIN_VALUE + dosageValues[ind])) / 100;
                if (genotypes[ind] == 0 && dosagevalue > 1) {
                    takeComplement = true;
                    break;
                }
                if (genotypes[ind] == 2 && dosagevalue < 1) {
                    takeComplement = true;
                    break;
                }
            }
            if (takeComplement) {
                for (int ind = 0; ind < dosageValues.length; ind++) {
                    byte dosageValue = (byte) (200 - (-Byte.MIN_VALUE + dosageValues[ind]) + Byte.MIN_VALUE);
                    dosageValues[ind] = dosageValue;
                }
            }

            snp.setDosage(dosageValues);
        }
    }

    /**
     * @return the numIndividuals
     */
    public int getNumIndividuals() {
        return m_numIndividuals;
    }

    /**
     * @param numIndividuals the numIndividuals to set
     */
    public void setNumIndividuals(int numIndividuals) {
        this.m_numIndividuals = numIndividuals;

    }

    public boolean hasDosageInformation() {
        return (m_dosagehandle != null);
    }

    public double getAverageSNPSize(int numSNPs) throws IOException {
        long size = 0;

        size += m_genotypehandle.length();
        if (m_dosagehandle != null) {
            size += m_dosagehandle.length();
        }


        double avgSNPSize = 0;
        if (size > 0) {
            avgSNPSize = (double) size / numSNPs;
        }

        return avgSNPSize;
    }

    public void close() throws IOException {
        if (m_dosagehandle != null) {
            m_dosagehandle.close();
        }
        m_genotypehandle.close();
    }
}
