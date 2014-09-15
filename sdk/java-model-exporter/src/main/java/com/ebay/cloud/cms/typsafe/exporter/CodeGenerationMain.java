package com.ebay.cloud.cms.typsafe.exporter;

import com.ebay.cloud.cms.typsafe.exporter.CodeGenerator.FileMetaJsonReader;
import com.ebay.cloud.cms.typsafe.exporter.CodeGenerator.MetaJsonReader;
import com.ebay.cloud.cms.typsafe.exporter.CodeGenerator.OnlineMetaJsonReader;

public class CodeGenerationMain {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int argLen = args == null ? 0 : args.length;
        if (argLen != 3 && argLen != 5) {
            printUsage();
            return;
        }

        String baseUrl;
        String repo;
        String outputDirectory;
        String packagePrefix;
        String token;
        String filePath;
        MetaJsonReader reader;
        if (argLen == 5) {
            baseUrl = args[0];
            repo = args[1];
            outputDirectory = args[2];
            packagePrefix = args[3];
            token = args[4];
            reader = new OnlineMetaJsonReader(baseUrl, repo, token);
        } else {
            filePath = args[0];
            outputDirectory = args[1];
            packagePrefix = args[2];
            reader = new FileMetaJsonReader(filePath);
        }

        CodeGenerator generator = new CodeGenerator(reader, outputDirectory, packagePrefix);
        generator.generate();
    }

    private static void printUsage() {
        System.out
                .println("Online generation: input 5 parameters:ServerUrl, repotitoryName, outputDirectory, packagePrefix, authorizationToken, like (http://localhost:9090/cms raptor-paas ./ com.ebay.cloud.cms.model user:fjao)");

        System.out.println();

        System.out
                .println("Local generation: input 3 parameters: MetaJsonFileFolderPath, outputDirectory, packagePrefix, like (c:\\raptor-paas-meta.json ./ com.ebay.cloud.cms.model user:fjao)");
    }

}
