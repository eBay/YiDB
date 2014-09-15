package com.ebay.cloud.cms.typsafe.exporter;

import com.ebay.cloud.cms.web.RunTestServer;

public class CodeGeneratorMainTest {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // start server
        RunTestServer.startServer(new String[] { "", "9000" });

        CodeGenerationMain.main(args);

        // stop server
        RunTestServer.stopServer();

    }

}
