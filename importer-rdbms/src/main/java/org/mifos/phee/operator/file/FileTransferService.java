package org.mifos.phee.operator.file;

public interface FileTransferService {

    String downloadFile(String fileName, String bucketName);

}
