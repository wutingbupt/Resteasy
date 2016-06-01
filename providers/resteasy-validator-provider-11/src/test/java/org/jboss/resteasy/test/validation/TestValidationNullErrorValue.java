package org.jboss.resteasy.test.validation;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.test.EmbeddedContainer;
import org.junit.Test;

/**
*
* @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
* @version $Revision: 1.1 $
*
* Created August 14, 2013
*/
public class TestValidationNullErrorValue
{
   protected static ResteasyDeployment deployment;
   protected static Dispatcher dispatcher;
   protected static Client client;

   @Path("")
   public static class TestResourceWithNullFieldAndProperty
   {
      @NotNull private String s;
      
      @Path("get")
      @GET
      public void doGet()
      {
      }
      
      @NotNull
      public String getT()
      {
         return null;
      }
 
      public void setS(String s)
      {
         this.s = s;
      }
   }
   
   @Path("")
   public static class TestResourceWithNullParameterAndReturnValue
   {
      @Path("post")
      @POST
      public void doPost(@NotNull @QueryParam("q") String q)
      {
      }

      @Path("get")
      @GET
      @NotNull
      public String doGet()
      {
         return null;
      }
   }

   //////////////////////////////////////////////////////////////////////////////
   @BeforeClass
   public static void beforeClass()
   {
      client = ClientBuilder.newClient();
   }
   
   @AfterClass
   public static void afterClass()
   {
      client.close();
   }
   
   public static void before(Class<?> resourceClass) throws Exception
   {
      after();
      deployment = EmbeddedContainer.start();
      dispatcher = deployment.getDispatcher();
      deployment.getRegistry().addPerRequestResource(resourceClass);
   }

   public static void after() throws Exception
   {
      EmbeddedContainer.stop();
      dispatcher = null;
      deployment = null;
   }

   //////////////////////////////////////////////////////////////////////////////
   @Test
   public void testNullFieldAndProperty() throws Exception
   {
      before(TestResourceWithNullFieldAndProperty.class);
      Builder request = client.target(generateURL("/get")).request();
      request.accept(MediaType.APPLICATION_XML);
      Response response = request.get();
      ViolationReport report = response.readEntity(ViolationReport.class);
      System.out.println("report: " + report.toString());
      countViolations(report, 2, 1, 1, 0, 0, 0);
      after();
   }
   
   @Test
   public void testNullParameterAndReturnValue() throws Exception
   {
      before(TestResourceWithNullParameterAndReturnValue.class);

      {
         // Null query parameter
         Builder request = client.target(generateURL("/post")).request();
         request.accept(MediaType.APPLICATION_XML);
         Response response = request.post(null);
         ViolationReport report = response.readEntity(ViolationReport.class);
         System.out.println("report: " + report.toString());
         countViolations(report, 1, 0, 0, 0, 1, 0);
      }

      {
         // Null return value
         Builder request = client.target(generateURL("/get")).request();
         request.accept(MediaType.APPLICATION_XML);
         Response response = request.get();
         ViolationReport report = response.readEntity(ViolationReport.class);
         System.out.println("report: " + report.toString());
         countViolations(report, 1, 0, 0, 0, 0, 1);
      }
      
      after();
   }
   
   private void countViolations(ViolationReport e, int totalCount, int fieldCount, int propertyCount, int classCount, int parameterCount, int returnValueCount)
   {
      Assert.assertEquals(fieldCount, e.getFieldViolations().size());
      Assert.assertEquals(propertyCount, e.getPropertyViolations().size());
      Assert.assertEquals(classCount, e.getClassViolations().size());
      Assert.assertEquals(parameterCount, e.getParameterViolations().size());
      Assert.assertEquals(returnValueCount, e.getReturnValueViolations().size());
   }
}