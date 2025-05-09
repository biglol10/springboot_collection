package com.alibou.booknetwork;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

public class JasyptEncryptionTest {

	public static void main(String[] args) {
		String valueToEncrypt = "postgres";
		String password = "jasypt";

		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		SimpleStringPBEConfig config = new SimpleStringPBEConfig();
		config.setPassword(password);
		config.setAlgorithm("PBEWithMD5AndDES");
		config.setKeyObtentionIterations("1000");
		config.setPoolSize("1");
		config.setProviderName("SunJCE");
		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
		config.setStringOutputType("base64");
		encryptor.setConfig(config);

		String encryptedValue = encryptor.encrypt(valueToEncrypt);
		System.out.println("암호화된 값: " + encryptedValue);
		System.out.println("복호화된 값: " + encryptor.decrypt(encryptedValue));
	}
}
