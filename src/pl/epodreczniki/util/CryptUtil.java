package pl.epodreczniki.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import pl.epodreczniki.model.User;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public final class CryptUtil {

	static{
		Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
		Log.e("JCE","using spongy castle");
	}
	
	public static final int KEY_LENGTH = 256;
	
	public static final int SALT_LENGTH = KEY_LENGTH / 8;
	
	public static final int ITERATION_COUNT = 10000;
	
	public static final String ALGORITHM = "PBKDF2WithHmacSha1";
	
	private CryptUtil(){		
	}
	
	public static String generateSalt64(){
		final SecureRandom random = new SecureRandom();
		byte[] salt = new byte[SALT_LENGTH];
		random.nextBytes(salt);
		return Base64.encodeToString(salt, Base64.DEFAULT);
	}	
	
	public static byte[] generateKey(String password,String salt64){
		if(!TextUtils.isEmpty(password) && !TextUtils.isEmpty(salt64)){
			try {
				KeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.decode(salt64, Base64.DEFAULT),ITERATION_COUNT,KEY_LENGTH);
				SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
				return keyFactory.generateSecret(spec).getEncoded();
			} catch (InvalidKeySpecException e) {			
				Log.e("CryptUtil","this should never happen (invalid spec)");
			} catch (NoSuchAlgorithmException e) {
				Log.e("CryptUtil","this should never happen (no algorithm)");
			}	
		}		
		return null;
	}
	
	public static String generateKey64(String password,String salt64){		
		if(!TextUtils.isEmpty(password) && !TextUtils.isEmpty(salt64)){
			try {
				KeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.decode(salt64, Base64.DEFAULT), ITERATION_COUNT, KEY_LENGTH);
				SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
				byte[] keyBytes = keyFactory.generateSecret(spec).getEncoded();
				return Base64.encodeToString(keyBytes, Base64.DEFAULT);
			} catch (NoSuchAlgorithmException e) {
				Log.e("CryptUtil","this should never happen (no algorithm)");
			} catch (InvalidKeySpecException e) {
				Log.e("CryptUtil","this should never happen (invalid spec)");
			}	
		}		
		return null;
	}
	
	public static boolean verifyPassword(String enteredPassword,User user){
		if(user==null){
			return false;
		}
		byte[] keyToTest = generateKey(enteredPassword, user.getSalt64());
		return Arrays.equals(keyToTest, Base64.decode(user.getPassword64(), Base64.DEFAULT));
	}
	
}
