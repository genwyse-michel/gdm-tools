package com.genwyse.aphp.gdmtools;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

import com.genwyse.tools.Parameter;
import com.genwyse.tools.Parameters;
import com.genwyse.tools.Tool;
import com.genwyse.tools.ToolException;

public abstract class GDMTool extends Tool {
  private static final Logger logger = Logger.getLogger(GDMTool.class);
  
  // Paramétrage de l'outil GDM
  protected static final String propOracleDb = "oracle.db";
  protected static final String propOracleUser = "oracle.user";
  protected static final String propOraclePwd = "oracle.pwd";
  
  protected static Parameters classParameters = new Parameters (
    Tool.classParameters,
    new Parameter[] {
      new Parameter (propOracleDb, true, "", "Base Oracle GDM Patients, sous la forme: serveur:port:SID (ex.: eclat:1521:GDMPAT)", true),
      new Parameter (propOracleUser, true, "", "Utilisateur Oracle GDM Patients", true),
      new Parameter (propOraclePwd, true, "", "Mot de passe Oracle GDM Patients", true),
  });
  
  // Données internes
  private Connection conn = null;

  
  public Connection getConnection() {
    return conn;
  }

  public GDMTool() {
    
  }
  
  public void connectDB (boolean forceReconnect) throws GDMDBException {
    if (conn==null || forceReconnect) {
      try {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        
        String dbServer = getProperty(propOracleDb);
        String dbUser = getProperty(propOracleUser);
        String dbPwd = getProperty(propOraclePwd);

        logger.info(logPrefix+"Connexion Oracle: " + dbServer + ", " + dbUser);
        conn = DriverManager.getConnection("jdbc:oracle:thin:@" + dbServer,dbUser,dbPwd);
        logger.debug(logPrefix+"Connexion Oracle OK");
      } catch (ClassNotFoundException e) {
        String mess = "Erreur interne: classe non trouvee: "+e.getMessage();
        logger.fatal(logPrefix+mess);
        throw new GDMDBException(mess, e);
      } catch (SQLException e) {
        String mess = "Impossible de se connecter a la base de donnees: "+e.getMessage();
        logger.error(logPrefix+mess);
        throw new GDMDBException(mess, e);
      }
      
    }
  }
  
  public void disconnectDB() {
    try {
      if (conn!=null && !conn.isClosed()) {
        conn.close();
        conn = null;
      }
    } catch (SQLException e) {
      String mess = "Erreur lors de la deconnexion de la base de donnees: "+e.getMessage();
      logger.error(logPrefix+mess);
    }
  }
  
  /*
   * Donne la date système selon le serveur de base de données
   */
  public Date getDBTime() throws GDMToolException {
    try {
      Statement stmt = getConnection().createStatement();
      ResultSet rs = stmt.executeQuery("SELECT sysdate as now FROM DUAL");
      java.sql.Date sqlDate = rs.getDate("now");
      return new Date(sqlDate.getTime());
    } catch (SQLException e) {
      logger.error("Erreur en interrgeant la date Oracle: "+e.getMessage());
      throw new GDMToolException(e);
    }
  }
  
  @Override
  public boolean initialize(String[] args) throws ToolException, IOException {
    if (!super.initialize(args)) {
      return false;
    }
    
    // Autres initialisations selon configuration
    
    return true;
  }
  
  public static void main(String[] args) {
    try {
      main (GDMTool.class, GDMTool.classParameters, args);
    } catch (ToolException e) {
      e.printStackTrace();
    }
  }
}
