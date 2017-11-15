package org.hornsby;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.Random;

/**
 * Created by jiang on 28/08/2017.
 */
public class Data {
  public static final String urlBase_aud ="http://service.fx168.com/cftc/GetData.ashx?code=AUSTRALIAN%20DOLLAR%20-%20CHICAGO%20MERCANTILE%20EXCHANGE&date=";
  public static final String urlBase_eur = "http://service.fx168.com/cftc/GetData.ashx?code=EURO%20FX%20-%20CHICAGO%20MERCANTILE%20EXCHANGE&date=";
  public static final String getUrlBase_gbp =   "http://service.fx168.com/cftc/GetData.ashx?code=BRITISH%20POUND%20STERLING%20-%20CHICAGO%20MERCANTILE%20EXCHANGE&date=";
  public static final String getUrlBase_cad = "http://service.fx168.com/cftc/GetData.ashx?code=CANADIAN%20DOLLAR%20-%20CHICAGO%20MERCANTILE%20EXCHANGE&date=";
  public static final String getUrlBase_jpy = "http://service.fx168.com/cftc/GetData.ashx?code=JAPANESE%20YEN%20-%20CHICAGO%20MERCANTILE%20EXCHANGE&date=";
  public static final String getUrlBase_nzd = "http://service.fx168.com/cftc/GetData.ashx?code=NEW%20ZEALAND%20DOLLAR%20-%20CHICAGO%20MERCANTILE%20EXCHANGE&date=";
  public static final String getUrlBase_chf="http://service.fx168.com/cftc/GetData.ashx?code=SWISS%20FRANC%20-%20CHICAGO%20MERCANTILE%20EXCHANGE&date=";

  enum DataSource{
    gbp("GBP",getUrlBase_gbp,"data_gbp.txt"),
    cad("CAD",getUrlBase_chf,"data_cad.txt"),
    jpy("JPY",getUrlBase_jpy,"data_jpy.txt"),
    nzd("NZD",getUrlBase_nzd,"data_nzd.txt"),
    chf("CGF",getUrlBase_chf,"data_chf.txt"),
    aud("AUD",urlBase_aud,"data_au.txt"),
    eur("EUR",urlBase_eur,"data_eur.txt");

    public final String url;
    public final String fileName;
    public final String currency;
    DataSource(String currency,String url, String fileName) {
      this.currency = currency;
      this.url = url;
      this.fileName = fileName;
    }
  }

  public static final LocalDate endDate = LocalDate.now();
  public static void main(String[] args) throws Exception {
    Proxy proxy;
    if(args.length>0 && args[0].equals("dir")){
      proxy = Proxy.NO_PROXY;
    }else{
      proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress("proxy.nab.com.au",10091));
    }

    Random random = new Random(30);
    for(DataSource source:DataSource.values()) {
      String fileName = source.fileName;
      LocalDate date = getLatestDate(fileName);
      System.out.println("start to pull "+source.currency+" "+date);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
      while (date.isBefore(endDate)) {
        try {
          String dateStr = getDateStr(date);
          URL url = new URL(source.url + dateStr);
          URLConnection connection = url.openConnection(proxy);
          BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
          String line = null;
          while ((line = reader.readLine()) != null) {
            writer.write(dateStr);
            writer.write("@");
            writer.write("CHICAGO");
            writer.write("@");
            writer.write(line);
            writer.write("\r\n");
          }
          writer.flush();
          System.out.println(date + " is done.");
          int timeToSleep = 0;
          while (timeToSleep < 5) {
            timeToSleep = random.nextInt(60);
          }
          Thread.sleep(timeToSleep * 1000L);
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          date = date.plusDays(1);
        }
      }
    }

  }


  public static final String getDateStr(LocalDate localDate){
    return localDate.get(ChronoField.YEAR)+"-"+localDate.get(ChronoField.MONTH_OF_YEAR)+"-"+localDate.get(ChronoField.DAY_OF_MONTH);
  }

  private static final LocalDate earilestDate = LocalDate.of(2007,01,30);
  public static final String format = "yyyy-M-d";
  private static LocalDate getLatestDate(String fileName) throws Exception {
    File file = new File(fileName);
    if(!file.exists() || file.length()<500){
      return earilestDate;
    }
    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile= new RandomAccessFile(fileName, "r");
      StringBuilder builder = new StringBuilder();
      long length = file.length();
      length-=2;
      randomAccessFile.seek(length);
      for(long seek = length; seek >= 0; --seek){
        randomAccessFile.seek(seek);
        char c = (char)randomAccessFile.read();
        if(c == '\n'){
          break;
        }
        builder.append(c);
      }
      builder = builder.reverse();
      String dataLine = builder.toString();
      int index = dataLine.indexOf('@');
      if(index==-1){
        return earilestDate;
      }else{
        String dateStr = dataLine.substring(0,index);
        return LocalDate.parse(dateStr,DateTimeFormatter.ofPattern(format)).plus(1, ChronoUnit.DAYS);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return earilestDate;
  }
}
