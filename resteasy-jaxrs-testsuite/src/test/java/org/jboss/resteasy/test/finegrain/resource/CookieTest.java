package org.jboss.resteasy.test.finegrain.resource;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.test.EmbeddedContainer;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.resteasy.test.TestPortProvider.generateBaseUrl;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieTest
{
   private static Dispatcher dispatcher;
   private static Client client;

   @Path("/")
   public static class CookieResource
   {
      @Path("/set")
      @GET
      public Response set()
      {
         return Response.ok("content").cookie(new NewCookie("meaning", "42")).build();
      }

      @Context
      HttpHeaders myHeaders;

      @Path("/headers")
      @GET
      public String headers(@Context HttpHeaders headers)
      {
         String value = headers.getCookies().get("meaning").getValue();
         Assert.assertEquals(value, "42");
         return value;
      }

      @Path("/headers/fromField")
      @GET
      public String headersFromField(@Context HttpHeaders headers)
      {
         String value = myHeaders.getCookies().get("meaning").getValue();
         Assert.assertEquals(value, "42");
         return value;
      }

      @Path("/param")
      @GET
      @Produces("text/plain")
      public int param(@CookieParam("meaning") int value)
      {
         Assert.assertEquals(value, 42);
         return value;
      }

      @Path("/cookieparam")
      @GET
      public String param(@CookieParam("meaning") Cookie value)
      {
         Assert.assertEquals(value.getValue(), "42");
         return value.getValue();
      }

      @Path("/default")
      @GET
      @Produces("text/plain")
      public int defaultValue(@CookieParam("defaulted") @DefaultValue("24") int value)
      {
         Assert.assertEquals(value, 24);
         return value;
      }
   }

   @BeforeClass
   public static void before() throws Exception
   {
      dispatcher = EmbeddedContainer.start().getDispatcher();
      dispatcher.getRegistry().addPerRequestResource(CookieResource.class);
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void after() throws Exception
   {
      client.close();
      EmbeddedContainer.stop();
   }

   private void _test(WebTarget target)
   {
      Response response = null;
      try
      {
         response = target.request().get();
         Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
         MultivaluedMap<String, Object> headers = response.getHeaders();
         for (Object key : headers.keySet())
         {
            System.out.println(key + ": " + headers.get(key));
         }
      } catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      finally
      {
         response.close();
      }
   }

   @Test
   public void testIt()
   {
      WebTarget target = client.target(generateBaseUrl());
      _test(target.path("/set"));
      _test(target.path("/headers"));
      _test(target.path("/headers/fromField"));
      _test(target.path("/cookieparam"));
      _test(target.path("/param"));
      _test(target.path("/default"));
   }

   public static interface CookieProxy
   {
      @Path("/param")
      @GET
      public int param(@CookieParam("meaning") int value);

      @Path("/param")
      @GET
      public int param(Cookie cookie);
   }

   @Test
   public void testProxy()
   {
      ResteasyWebTarget target = (ResteasyWebTarget) client.target(generateBaseUrl());
      {
         CookieProxy proxy = target.proxy(CookieProxy.class);
         proxy.param(42);
      }
      {
         CookieProxy proxy = target.proxy(CookieProxy.class);
         Cookie cookie = new Cookie("meaning", "42");
         proxy.param(cookie);
      }

   }

   private boolean testCookie(Cookie ck, String name, String value,
                              String path, String domain, int version) throws Exception
   {

      StringBuffer sb = new StringBuffer();
      boolean pass = true;

      if (name == "" || name == null)
      {
         pass = false;
         sb.append("Cookie's name is empty");
      }
      else if (!ck.getName().toLowerCase().equals(name))
      {
         pass = false;
         sb.append("Failed name test.  Expect " + name + " got " +
                 ck.getName());
      }

      if (value == "" || value == null)
      {
         pass = false;
         sb.append("Cookie's value is empty");
      }
      else if (!ck.getValue().toLowerCase().equals(value))
      {
         pass = false;
         sb.append("Failed value test.  Expect " + value + " got " +
                 ck.getValue());
      }

      if (ck.getVersion() != version)
      {
         pass = false;
         sb.append("Failed version test.  Expect " + version + " got " +
                 ck.getVersion());
      }

      if (path == "" || path == null)
      {
         if (ck.getPath() != "" && ck.getPath() != null)
         {
            pass = false;
            sb.append("Failed path test.  Expect null String, got " +
                    ck.getPath());
         }
      }
      else if (ck.getPath() == null || ck.getPath() == "")
      {
         pass = false;
         sb.append("Failed path test.  Got null, expecting " + path);
      }
      else if (!ck.getPath().toLowerCase().equals(path))
      {
         pass = false;
         sb.append("Failed path test.  Expect " + path + " got " +
                 ck.getPath());
      }

      if (domain == "" || domain == null)
      {
         if (ck.getDomain() != "" && ck.getDomain() != null)
         {
            pass = false;
            sb.append("Failed path test.  Expect " + domain + " got " +
                    ck.getDomain());
         }
      }
      else if (!ck.getDomain().toLowerCase().equals(domain))
      {
         pass = false;
         sb.append("Failed domain test.  Expect " + domain + " got " +
                 ck.getDomain());
      }

      if (!pass)
      {
         throw new Exception("At least one assertion falied: " + sb.toString());
      }

      return pass;
   }

   /*
   * Create a version 0 Cookie instance by Parsing a String
   */
   @Test
   @SuppressWarnings("unused")
   public void testParse1() throws Exception
   {
      boolean pass = true;

      String cookie_toParse = "NAME_1=Value_1;";
      String name = "name_1";
      String value = "value_1";
      String path = "";
      String domain = "";
      int version = 0;

      Cookie ck6 = javax.ws.rs.core.Cookie.valueOf(cookie_toParse);
      pass = testCookie(ck6, name, value, path, domain, version);
   }

   /*
   * Create a version 0 Cookie instance by Parsing a String
   */
   @Test
   @SuppressWarnings("unused")
   public void testParse2() throws Exception
   {
      boolean pass = true;
      String cookie_toParse =
              "$Version=\"1\"; Customer=\"WILE_E_COYOTE\"; $Path=\"/acme\"";

      String name = "customer";
      String value = "wile_e_coyote";
      String path = "/acme";
      String domain = "";
      int version = 1;

      Cookie ck7 = javax.ws.rs.core.Cookie.valueOf(cookie_toParse);

      pass = testCookie(ck7, name, value, path, domain, version);
   }

   /*
   * Testing Correcting exception thrown
   *                 when calling Cookie.valueOf(null)
   */
   @Test
   @SuppressWarnings("unused")
   public void testParse3() throws Exception
   {

      try
      {
         Cookie ck8 = javax.ws.rs.core.Cookie.valueOf(null);
         throw new Exception("Expectecd IllegalArgumentException not thrown.  Test Failed");
      }
      catch (java.lang.IllegalArgumentException ilex)
      {
      }
   }

   /*
     * Create a version 0 Cookie instance by Parsing a String
     */
   @Test
   @SuppressWarnings("unused")
   public void testNewCookie1() throws Exception
   {
      boolean pass = true;

      String NewCookie_toParse = "NAME_1=Value_1;";
      String name = "name_1";
      String value = "value_1";
      String path = "";
      String domain = "";
      int version = 1;

      NewCookie nck26 = javax.ws.rs.core.NewCookie.valueOf(NewCookie_toParse);

      pass = verifyNewCookie(nck26, name, value, path, domain, version,
              "", -1, null, false, false);

     // check round-tripping
     Assert.assertEquals(nck26, NewCookie.valueOf(nck26.toString()));
   }

   /*
   * Create a version 1 NewCookie instance by Parsing a String
   */
   @Test
   @SuppressWarnings("unused")
   public void testNewCookie2() throws Exception
   {
      boolean pass = true;
      String NewCookie_toParse =
              "Customer=WILE_E_COYOTE; Path=/acme; Version=1";

      String name = "customer";
      String value = "wile_e_coyote";
      String path = "/acme";
      String domain = "";
      int version = 1;

      NewCookie nck27 = javax.ws.rs.core.NewCookie.valueOf(NewCookie_toParse);

      pass = verifyNewCookie(nck27, name, value, path, domain, version,
              "", -1, null, false, false);

     // check round-tripping
     Assert.assertEquals(nck27, NewCookie.valueOf(nck27.toString()));
   }

   /*
   * Create a NewCookie instance by Parsing a null String.
   *                 Verify that IllegalArgumentException is thrown
   */
   @Test
   @SuppressWarnings("unused")
   public void testNewCookie3() throws Exception
   {
      try
      {
         NewCookie nck27 = javax.ws.rs.core.NewCookie.valueOf(null);
         throw new Exception("Expected IllegalArgumentException not thrown. Test Failed.");
      }
      catch (IllegalArgumentException ilex)
      {

      }
   }


   /*
    * Create a version 0 NewCookie instance by Parsing a String
    */
   @Test
   @SuppressWarnings("unused")
   public void testNewCookie4() throws Exception
   {
      boolean pass = true;
      String NewCookie_toParse =
              "Customer=WILE_E_COYOTE; Path=/acme; Domain=acme.com; Max-Age=150000000; " +
              "Expires=Thu, 03-May-2018 10:36:34 GMT; Secure; HttpOnly";

      String name = "customer";
      String value = "wile_e_coyote";
      String path = "/acme";
      String domain = "acme.com";
      int version = 1;
      String comment = "";
      int maxAge = 150000000;
      Date expiry = new Date(Date.UTC(118, 4, 3, 10, 36, 34));
      boolean secure = true;
      boolean httpOnly = true;

      NewCookie nck28 = javax.ws.rs.core.NewCookie.valueOf(NewCookie_toParse);

      pass = verifyNewCookie(nck28, name, value, path, domain, version,
              comment, maxAge, expiry, secure, httpOnly);

      // check round-tripping
      Assert.assertEquals(nck28, NewCookie.valueOf(nck28.toString()));
   }

   private boolean verifyNewCookie(NewCookie nck, String name, String value,
                                   String path, String domain, int version, String comment, int maxage,
                                   Date expiry, boolean secure, boolean httpOnly) throws Exception
   {

      StringBuffer sb = new StringBuffer();
      boolean pass = true;

      if (name == null || name.isEmpty())
      {
         pass = false;
         sb.append("NewCookie's name is empty");
      }
      else if (!nck.getName().toLowerCase().equals(name))
      {
         pass = false;
         sb.append("Failed name test.  Expect " + name + " got " +
                 nck.getName());
      }

      if (value == null || value.isEmpty())
      {
         pass = false;
         sb.append("NewCookie's value is empty");
      }
      else if (!nck.getValue().toLowerCase().equals(value))
      {
         pass = false;
         sb.append("Failed value test.  Expect " + value + " got " +
                 nck.getValue());
      }

      if (nck.getVersion() != version)
      {
         pass = false;
         sb.append("Failed version test.  Expect " + version + " got " +
                 nck.getVersion());
      }

      if (comment == null || comment.isEmpty())
      {
         if (nck.getComment() != "" && nck.getComment() != null)
         {
            pass = false;
            sb.append("Failed comment test.  Expect null String, got <" +
                    nck.getComment() + ">");
         }
      }
      else if (!nck.getComment().toLowerCase().equals(comment))
      {
         pass = false;
         sb.append("Failed comment test.  Expect " + comment + " got " +
                 nck.getComment());
      }

      if (path == null || path.isEmpty())
      {
         if (nck.getPath() != "" && nck.getPath() != null)
         {
            pass = false;
            sb.append("Failed path test.  Expect null String, got " +
                    nck.getPath());
         }
      }
      else if (nck.getPath() == null || nck.getPath().isEmpty())
      {
         pass = false;
         sb.append("Failed path test.  Got null, expecting " + path);
      }
      else if (!nck.getPath().toLowerCase().equals(path))
      {
         pass = false;
         sb.append("Failed path test.  Expect " + path + " got " +
                 nck.getPath());
      }

      if (domain == null || domain.isEmpty())
      {
         if (nck.getDomain() != null && !nck.getDomain().isEmpty())
         {
            pass = false;
            sb.append("Failed domain test.  Expect " + domain + " got " +
                    nck.getDomain());
         }
      }
      else if (!nck.getDomain().toLowerCase().equals(domain))
      {
         pass = false;
         sb.append("Failed domain test.  Expect " + domain + " got " +
                 nck.getDomain());
      }

      if (nck.getMaxAge() != maxage)
      {
         pass = false;
         sb.append("Failed maxage test.  Expect " + maxage + " got " +
                 nck.getMaxAge());
      }

      if (expiry == null)
      {
         if (nck.getExpiry() != null)
         {
            pass = false;
            sb.append("Failed expiry test.  Expect " + expiry + " got " +
                    nck.getExpiry());
         }
      }
      else if (nck.getExpiry() == null)
      {
         pass = false;
         sb.append("Failed expiry test.  Got null, expecting " + expiry);
      }
      else if (!nck.getExpiry().equals(expiry)) {
         pass = false;
         sb.append("Failed expirytest.  Expect " + expiry + " got " +
                 nck.getExpiry());
      }

      if (nck.isSecure() != secure)
      {
         pass = false;
         sb.append("Failed secure test.  Expect " + secure + " got " +
                 nck.isSecure());
      }

      if (nck.isHttpOnly() != httpOnly)
      {
         pass = false;
         sb.append("Failed httpOnly test.  Expect " + httpOnly + " got " +
                 nck.isHttpOnly());
      }

      if (!pass)
      {
         throw new Exception("At least one assertion failed: " + sb.toString());
      }

      return pass;
   }

   /*
   * Create an instance of Response using
   *                 Response.ResponseBuilder.cookie(NewCookie).build()
   *                 verify that correct status code is returned
   *
   */
   @Test
   @SuppressWarnings("unused")
   public void cookieTest5() throws Exception
   {
      StringBuffer sb = new StringBuffer();
      boolean pass = true;

      int status = 200;

      String name = "name_1";
      String value = "value_1";
      int maxage = javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;
      int version = 1;
      boolean secure = false;

      Cookie ck1 = new Cookie(name, value);
      NewCookie nck1 = new NewCookie(ck1);

      name = "name_2";
      value = "value_2";
      String path = "/acme";
      String domain = "";

      Cookie ck2 = new Cookie(name, value, path, domain);
      NewCookie nck2 = new NewCookie(ck2);

      name = "name_3";
      value = "value_3";
      path = "";
      domain = "y.x.foo.com";

      Cookie ck3 = new Cookie(name, value, path, domain);
      NewCookie nck3 = new NewCookie(ck3);

      List<String> cookies = Arrays.asList(nck1.toString().toLowerCase(),
              nck2.toString().toLowerCase(),
              nck3.toString().toLowerCase());

      Response resp = Response.status(status).cookie(nck1, nck2, nck3).build();
      String tmp = verifyResponse(resp, null, status, null, null, null, null,
              null, cookies);
      if (tmp.endsWith("false"))
      {
         pass = false;
      }
      sb.append(tmp + "\n");
      System.out.println(sb.toString());
      if (!pass)
      {
         throw new Exception("AT least one assertion failed.");
      }
   }

   /*
   * Create an instance of Response using
   *                 Response.ResponseBuilder.header(String, Object).build()
   *                 verify that correct status code is returned
   *
   */
   @Test
   public void testHeader() throws Exception
   {
      StringBuffer sb = new StringBuffer();
      boolean pass = true;

      int status = 200;
      List<String> type = Arrays.asList("text/plain", "text/html");
      List<String> encoding = Arrays.asList("gzip", "compress");
      List<String> lang = Arrays.asList("en_US", "en_GB", "zh_CN");

      String name = "name_1";
      String value = "value_1";
      Cookie ck1 = new Cookie(name, value);
      NewCookie nck1 = new NewCookie(ck1);

      List<String> cookies = Arrays.asList(nck1.toString().toLowerCase());

      Response resp = Response.status(status).header("Content-type",
              "text/plain").header("Content-type", "text/html").header("Content-Language", "en_US").
              header("Content-Language", "en_GB").header("Content-Language",
              "zh_CN").header("Cache-Control", "no-transform").
              header("Set-Cookie", "name_1=value_1;version=1").build();
      String tmp = verifyResponse(resp, null, status, encoding, lang, type,
              null, null, cookies);
      if (tmp.endsWith("false"))
      {
         pass = false;
      }
      sb.append(tmp + newline);
      System.out.println(sb.toString());
      if (!pass)
      {
         throw new Exception("AT least one assertion failed.");
      }
   }

   private static String newline = "\n";
   private static String indent = "\t";

   @SuppressWarnings("unused")   
   private String verifyResponse(Response resp, String content, int status,
                                 HashMap<String, String> expected_map) throws Exception
   {
      boolean pass = true;
      StringBuffer sb = new StringBuffer();

      sb.append("========== Verifying a Response with Map: " + newline);

      String entity = (String) resp.getEntity();

      if ((content == null) || (content == ""))
      {
         if (!(resp.getEntity() == null) || (resp.getEntity() == ""))
         {
            pass = false;
            sb.append(indent +
                    "Entity verification failed: expecting no content, got " +
                    (String) resp.getEntity() + newline);
         }
      }
      else if (!content.equals(((String) resp.getEntity())))
      {
         pass = false;
         sb.append(indent + "Entity verification failed: expecting " +
                 content +
                 ", got " + (String) resp.getEntity() + newline);
      }
      else
      {
         sb.append(indent + "Correct content found in Response: " +
                 (String) resp.getEntity() + newline);
      }

      if (resp.getStatus() != status)
      {
         pass = false;
         sb.append(indent + "Status code verification failed: expecting " +
                 status +
                 ", got " + resp.getStatus() + newline);
      }
      else
      {
         sb.append(indent + "Correct status found in Response: " + status +
                 newline);
      }

      MultivaluedMap<java.lang.String, java.lang.Object> mvp =
              resp.getMetadata();
      if (expected_map == null)
      {
         sb.append(indent +
                 "No keys to verify or expected, but found the following keys in Response:" +
                 newline);
         for (String key : mvp.keySet())
         {
            sb.append(indent + indent + "Key: " + key + "; " +
                    mvp.getFirst(key) + ";" + newline);
         }
      }
      else
      {
         for (String key_actual : mvp.keySet())
         {
            sb.append(indent + "Response contains key: " +
                    key_actual + newline);
         }
         sb.append(indent + "Verifying the following keys in Response:" +
                 newline);
         String actual, expected = null;
         for (Map.Entry<String, String> entry : expected_map.entrySet())
         {
            String key = entry.getKey();
            if (!mvp.containsKey(key))
            {
               pass = false;
               sb.append(indent + indent + "Key: " + key +
                       " is not found in Response;" + newline);
            }
            else if (key.equalsIgnoreCase("last-modified"))
            {
               sb.append(indent + indent +
                       "Key Last-Modified is found in response" +
                       newline);
            }
            else
            {
               expected = entry.getValue().toLowerCase();
               actual = mvp.getFirst(key).toString().toLowerCase();

               if (actual.startsWith("\"") && actual.endsWith("\""))
               {
                  actual = actual.substring(1, actual.length() - 1);
               }

               if (!actual.equals(expected))
               {
                  pass = false;
                  sb.append(indent + indent + "Key: " + key +
                          " found in Response, but with different value;" +
                          newline);
                  sb.append(indent + indent + "Expecting " +
                          entry.getValue() +
                          "; got " + mvp.getFirst(key) + newline);
               }
               sb.append(indent + indent + "Processed key " + key +
                       " with expected value " +
                       entry.getValue() + newline);
            }
         }
      }
      sb.append(indent + pass);
      return sb.toString();
   }

   private String verifyResponse(Response resp, String content, int status,
                                 List<String> encoding, List<String> language, List<String> type,
                                 List<String> var, List<String> ccl, List<String> cookies)
           throws Exception
   {
      boolean pass = true;
      StringBuffer sb = new StringBuffer();

      sb.append("========== Verifying a Response: " + newline);

      String tmp = verifyResponse(resp, content, status, null);
      sb.append(indent + tmp + newline);
      if (tmp.endsWith("false"))
      {
         pass = false;
      }

      MultivaluedMap<java.lang.String, java.lang.Object> mvp =
              resp.getMetadata();


      for (Map.Entry<String, List<Object>> entry : mvp.entrySet())
      {
         String key = entry.getKey();
         sb.append(indent + "Processing Key found in response: " + key + ": " +
                 entry.getValue() + "; " + newline);

         if (key.equalsIgnoreCase("Vary"))
         {
            for (String value : var)
            {
               String actual = entry.getValue().toString().toLowerCase();
               if (actual.indexOf(value) < 0)
               {
                  pass = false;
                  sb.append(indent + indent + "Expected header " + value +
                          " not set in Vary." + newline);
               }
               else
               {
                  sb.append(indent + indent + "Found expected header " +
                          value + "." + newline);
               }
            }
         }

         if (encoding != null)
         {
            if (key.toString().equalsIgnoreCase("Content-encoding"))
            {
               for (Object enc : entry.getValue())
               {
                  if (!encoding.contains(enc.toString().toLowerCase()))
                  {
                     pass = false;
                     sb.append(indent + indent + "Encoding test failed: " +
                             newline);
                  }
               }
            }
         }

         if (language != null)
         {
            if (key.toString().equalsIgnoreCase("Content-language"))
            {
               for (Object lang : entry.getValue())
               {
                  if (!language.contains(lang.toString()))
                  {
                     pass = false;
                     sb.append(indent + indent + "language test failed: " +
                             lang +
                             " is not expected in Response" + newline);
                     for (String tt : language)
                     {
                        sb.append(indent + indent +
                                "Expecting Content-Language " + tt +
                                newline);
                     }
                  }
               }
            }
         }


         if (type != null)
         {
            if (key.toString().equalsIgnoreCase("Content-Type"))
            {
               for (Object lang : entry.getValue())
               {
                  if (!type.contains(lang.toString().toLowerCase()))
                  {
                     pass = false;
                     sb.append(indent + indent +
                             "Content-Type test failed: " + lang +
                             " is not expected in Response" + newline);
                  }
               }
            }
         }

         if (ccl != null)
         {
            for (String tt : ccl)
            {
               sb.append("Expecting Cache-Control " + tt + newline);
            }
            if (key.toString().equalsIgnoreCase("Cache-Control"))
            {
               for (Object all_ccl : entry.getValue())
               {
                  for (String cc : ccl)
                  {
                     if (!(all_ccl.toString().toLowerCase().indexOf(cc.toLowerCase()) >
                             -1))
                     {
                        pass = false;
                        sb.append(indent + indent +
                                "Cache-Control test failed: " + cc +
                                " is not found in Response." + newline);
                     }
                  }
               }
            }
         }

         if (cookies != null)
         {
            for (String tt : cookies)
            {
               sb.append(indent + indent + "EXpecting Set-Cookie" + tt +
                       newline);
            }
            if (key.toString().equalsIgnoreCase("Set-Cookie"))
            {
               for (Object nck_actual : entry.getValue())
               {
                  sb.append(indent + indent + "Processing " +
                          nck_actual.toString() +
                          newline);
                  String s = nck_actual.toString().toLowerCase().
                          replace(" ", "");
                  if (!cookies.contains(s))
                  {
                     pass = false;
                     sb.append(indent + indent +
                             "Set-Cookie test failed: " + nck_actual +
                             " is not expected in Response." + newline);
                  }
                  else
                  {
                     sb.append(indent + indent + "Expected Set-Cookie: " +
                             nck_actual +
                             " is found in Response." + newline);
                  }
               }
            }
         }
      }
      sb.append(indent + pass);

      return sb.toString();
   }


}
