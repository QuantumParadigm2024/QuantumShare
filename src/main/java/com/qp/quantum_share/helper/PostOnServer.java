package com.qp.quantum_share.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.qp.quantum_share.exception.CommonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Service
public class PostOnServer {

    @Autowired
    FileConvertion fileConvertion;

    private static final String SFTP_HOST = "pdx1-shared-a2-03.dreamhost.com";
    private static final int SFTP_PORT = 22;
    private static final String SFTP_USER = "dh_q2m9hh";
    private static final String SFTP_PASSWORD = "SriKrishna@0700";
    private static final String SFTP_DIRECTORY = "/home/dh_q2m9hh/quantumshare.quantumparadigm.in/";
    private static final String BASE_URL = "https://quantumshare.quantumparadigm.in/";

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public List<String> uploadFile(MultipartFile[] files, String directory) {
        CompletableFuture<Void>[] futures = new CompletableFuture[files.length];
        List<String> url = new ArrayList<String>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            try {
                byte[] fileBytes = file.getBytes();
                String originalFilename = file.getOriginalFilename();
                String uniqueFileName = generateUniqueFileName(originalFilename);

                futures[i] = CompletableFuture.runAsync(() -> {
                    String fileUrl = uploadFileViaSFTP(fileBytes, uniqueFileName, directory);
                    if (fileUrl != null) {
                        url.add(fileUrl);
                    }
                }, executorService);
            } catch (IOException e) {
                new RuntimeException("File Not supported");
            }
        }
        try {
            CompletableFuture.allOf(futures).get();
        } catch (Exception e) {
            new RuntimeException(e.getMessage());
        }
        return url;
    }

    private String uploadFileViaSFTP(byte[] fileBytes, String fileName, String directory) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            try (InputStream fileInputStream = new ByteArrayInputStream(fileBytes)) {
                sftpChannel.put(fileInputStream, SFTP_DIRECTORY + directory + fileName);
            }
            return BASE_URL + directory + fileName;
        } catch (JSchException | SftpException | IOException e) {
            return null;
        } finally {
            if (sftpChannel != null) {
                try {
                    sftpChannel.disconnect();
                } catch (Exception e) {
                    return null;
                }
            }
            if (session != null) {
                try {
                    session.disconnect();
                } catch (Exception e) {
                    return null;
                }
            }
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        originalFilename = originalFilename.replaceAll("\\s+", "");
        return UUID.randomUUID().toString() + originalFilename;
    }

    public void deleteFile(String fileUrl, String path) {
        String fileName = fileUrl.replace(BASE_URL + path, "");
        String remoteFilePath = SFTP_DIRECTORY + path + fileName;
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            sftpChannel.rm(remoteFilePath);

        } catch (JSchException | SftpException e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
