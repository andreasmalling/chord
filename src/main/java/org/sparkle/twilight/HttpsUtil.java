package org.sparkle.twilight;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

public class HttpsUtil extends HttpUtil {

    public HttpsUtil() {
        LOGGER = Logger.getLogger(HttpsUtil.class.getName());
        LoggerHandlers.addHandlers(LOGGER);
        try {
            client = new HttpsClientFactory().getHttpsClient();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
}
