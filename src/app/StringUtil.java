package app;

import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import com.google.gson.GsonBuilder;
import java.util.List;

public class StringUtil {

	// string 값에 sha256을 적용하여 해시값 리턴
	public static String applySha256(String input) {
		
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			// input값에 sha256 적용
			byte[] hash = digest.digest(input.getBytes("UTF-8"));

			StringBuffer hexString = new StringBuffer(); // 해시를 16진수로 저장하기 위한 스트링버퍼 인스턴스 생성
			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// ECDSA 서명을 적용하고 결과값 리턴 (bytes 값으로)
	public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
		Signature dsa;
		byte[] output = new byte[0];
		try {
			dsa = Signature.getInstance("ECDSA", "BC");
			dsa.initSign(privateKey);
			byte[] strByte = input.getBytes();
			dsa.update(strByte);
			byte[] realSig = dsa.sign();
			output = realSig;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return output;
	}

	// 서명 검증
	public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
		try {
			Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
			ecdsaVerify.initVerify(publicKey);
			ecdsaVerify.update(data.getBytes());
			return ecdsaVerify.verify(signature);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// Object를 json 데이터로 바꿔 리턴
	public static String getJson(Object o) {
		return new GsonBuilder().setPrettyPrinting().create().toJson(o);
	}

	// Returns difficulty string target, to compare to hash. eg difficulty of 5 will
	// return "00000"
	public static String getDificultyString(int difficulty) {
		return new String(new char[difficulty]).replace('\0', '0');
	}

	public static String getStringFromKey(Key key) {
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	public static String getMerkleRoot(ArrayList<Transaction> transactions) {
		int count = transactions.size();

		List<String> previousTreeLayer = new ArrayList<String>();
		for (Transaction transaction : transactions) {
			previousTreeLayer.add(transaction.transactionId);
		}
		List<String> treeLayer = previousTreeLayer;

		while (count > 1) {
			treeLayer = new ArrayList<String>();
			for (int i = 1; i < previousTreeLayer.size(); i += 2) {
				treeLayer.add(applySha256(previousTreeLayer.get(i - 1) + previousTreeLayer.get(i)));
			}
			count = treeLayer.size();
			previousTreeLayer = treeLayer;
		}

		String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
		return merkleRoot;
	}
}