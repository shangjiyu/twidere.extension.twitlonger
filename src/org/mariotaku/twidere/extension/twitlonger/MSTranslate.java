/**
 * @Title: MSTranslate.java
 * @Package org.mariotaku.twidere.extension.twitlonger
 * @Description: TODO(调用巨硬translate API)
 * @author shangjiyu@gmail.com
 * @date 2013-9-21 下午1:13:41
 * @version V1.0
 */

package org.mariotaku.twidere.extension.twitlonger;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * @ClassName: MSTranslate
 * @Description: TODO(发送POST请求，返回请求结果)
 * @author shangjiyu
 * @date 2013-9-21 下午1:13:41
 *
 */

public class MSTranslate {

	private static final String CLIEND_ID_STRING = "firefox_inline_translate";
	private static final String CLIEND_SECRET_STRING = "l/t7OPv/O+Ye68qESMF2RwN7pV+jajAxrW8YDrMbmoo=";
	private static final String SCOPE_STRING = "http://api.microsofttranslator.com";
	private static final String GRANT_TYPE_STRING = "client_credentials";
	private static final String DATAMARKETACCESSURL_STRING = "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13";
	private static final String MSTRANSLATEURL_STRING = "http://api.microsofttranslator.com/V2/Http.svc/Translate";
	private static final Pattern PATTERN_LINK = Pattern.compile("((RT\\s?)?(@([a-zA-Z0-9_\\u4e00-\\u9fa5]{1,20})):?)|((https?://)([-\\w\\.]+)+(:\\d+)?(/([\\w/_\\-\\.]*(\\?\\S+)?)?)?)|(\\#[a-zA-Z0-9_%\\u4e00-\\u9fa5]*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_ALPHA = Pattern.compile("(11111)");
	private final ArrayList<String> linkStrings = new ArrayList<String>();
	private int uneed2TranslateElementIndex = 0;
	
	public MSTranslate() {
		// TODO Auto-generated constructor stub
	}
	
	public TranslateResponse postTranslate(String srcContent) throws MSTranslateException, IllegalStateException, JSONException {
		try {
			String queryString = "";
			final Matcher matcher = PATTERN_LINK.matcher(srcContent);
			while (matcher.find()) {
				if (matcher.group(1) != null) {
					linkStrings.add(matcher.group(1));
				}else if (matcher.group(5) != null) {
					linkStrings.add(matcher.group(5));
				}else if (matcher.group(12) != null) {
					linkStrings.add(matcher.group(12));
				}
			}
			queryString = PATTERN_LINK.matcher(srcContent).replaceAll("11111");
			queryString = URLEncoder.encode(queryString,"UTF-8");
			final String access_tokenString = getAccessToken();
			final String getURL = MSTRANSLATEURL_STRING+"?"+"appid="+"&text="+queryString+"&to=zh-CHS&contentType=text/plain";
			final HttpClient httpclient = new DefaultHttpClient();
			final HttpGet httpGet = new HttpGet();
			httpGet.setURI(new URI(getURL));
			httpGet.setHeader("Authorization", "Bearer "+access_tokenString);
			final HttpResponse response = httpclient.execute(httpGet);
			return parseTranslateResponse(EntityUtils.toString(response.getEntity()));
		} catch ( Exception e) {
			throw new MSTranslateException(e);
		}
	}
	
	
	public String getAccessToken() throws MSTranslateException, ParseException, JSONException {
		try {
			final HttpClient httpclient = new DefaultHttpClient();
			final HttpPost httpPost = new HttpPost(DATAMARKETACCESSURL_STRING);
			final ArrayList<NameValuePair> args = new ArrayList<NameValuePair>();
			args.add(new BasicNameValuePair("client_id", CLIEND_ID_STRING));
			args.add(new BasicNameValuePair("client_secret", CLIEND_SECRET_STRING));
			args.add(new BasicNameValuePair("scope", SCOPE_STRING));
			args.add(new BasicNameValuePair("grant_type", GRANT_TYPE_STRING));
			httpPost.setEntity(new UrlEncodedFormEntity(args, HTTP.UTF_8));
			final HttpResponse response = httpclient.execute(httpPost);
			JSONObject responseJsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
			return responseJsonObject.getString("access_token");
		} catch (final IOException e) {
			// TODO: handle exception
			throw new MSTranslateException(e);
		}
		
	}
	
	/**
	 * @throws IOException 
	 * @throws XmlPullParserException 
	 * @Title: parseTranslateResponse
	 * @Description: TODO(解析巨硬返回的XML数据)
	 * @param @param response
	 * @param @return
	 * @param @throws JSONException    设定文件
	 * @return TranslateResponse    返回类型
	 * @throws
	 */
	public TranslateResponse parseTranslateResponse(String response) throws JSONException, XmlPullParserException, IOException {
		String from = "yangpi", to = "chinese", translateResult = "";
		final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		final XmlPullParser parser = factory.newPullParser();
		parser.setInput(new StringReader(response));
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			 if(eventType == XmlPullParser.TEXT) {
	        	  translateResult = parser.getText();
	          }
			eventType = parser.next();
		}
		translateResult = replaceURL(PATTERN_ALPHA, translateResult);
		return new TranslateResponse(from, to, translateResult);
	}
	
	public String replaceURL(Pattern pattern,String toReplaceString) {
		final Matcher matcher = pattern.matcher(toReplaceString);
		if (matcher.find()) {
			toReplaceString = matcher.replaceFirst(linkStrings.get(uneed2TranslateElementIndex));
			uneed2TranslateElementIndex++;
			return replaceURL(pattern, toReplaceString);
		}else {
			return toReplaceString;
		}
	}
	
	public static class MSTranslateException extends Exception {
		
		private static final long serialVersionUID = 1020016463204999157L;
		
		public MSTranslateException() {
			super();
		}
		
		public MSTranslateException(String detailMsg) {
			super(detailMsg);
		}
		
		public MSTranslateException(Throwable throwable) {
			super(throwable);
		}
		
		public MSTranslateException(String detailMsg, Throwable throwable) {
			super(detailMsg, throwable);
		}
	}
	
	/**
	 * @ClassName: TranslateResponse
	 * @Description: TODO(翻译结果对象)
	 * @author shangjiyu
	 * @date 2013-9-21 下午4:18:58
	 *
	 */
	public static class TranslateResponse {
		public final String from, to, translateResult;

		private TranslateResponse(String from, String to, String translateResult) {
			this.from = from;
			this.to = to;
			this.translateResult = translateResult;
		}
	}
}

