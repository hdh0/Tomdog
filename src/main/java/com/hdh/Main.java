package com.hdh;

import com.hdh.connector.HttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(HttpConnector.class);
        String host = "0.0.0.0";
        int port = 8080;
        try (HttpConnector connector = new HttpConnector(host, port)) {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Tomdog http server was shutdown.");
    }
}
