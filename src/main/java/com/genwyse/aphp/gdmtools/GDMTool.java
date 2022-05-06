package com.genwyse.aphp.gdmtools;

import java.io.IOException;
import java.sql.CallableStatement;
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
  protected static final String propDebugSql = "debug-sql";
  protected static final String propTempo = "duree-attente";
  protected static final String propOneShot = "execution-unique";
  
  protected static Parameters classParameters = new Parameters (
    Tool.classParameters,
    new Parameter[] {
      new Parameter (propOracleDb, true, "", "Base Oracle GDM Patients, sous la forme: serveur:port:SID (ex.: eclat:1521:GDMPAT)", true),
      new Parameter (propOracleUser, true, "", "Utilisateur Oracle GDM Patients", true),
      new Parameter (propOraclePwd, true, "", "Mot de passe Oracle GDM Patients", true),
      new Parameter (propDebugSql, false, "", "Active la trace des requêtes SQL", false, 0, null, null),
      new Parameter (propTempo, false, "60", "Durée d'attente entre les exécutions", true),
      new Parameter (propOneShot, false, "false", "Ne faire qu'une exécution du traitement", true, 0, null, null),
  });
  
  // Données internes
  private Connection conn = null;
  private boolean debugSql = false;
  private int dureeAttente = 60;
  private boolean oneShot = false;

  public void setStatus(GDMToolStatus status) {
    this.status = status.getValue();
  }
    
  public Connection getConnection() {
    return conn;
  }

  public boolean getOneShot() {
    return oneShot;
  }
  
  public int getDureeAttente() {
    return dureeAttente;
  }
  
  public boolean isDebugSql() {
    return debugSql;
  }

  public void setDebugSql(boolean debugSql) {
    this.debugSql = debugSql;
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
        conn.setAutoCommit(false);
        logger.debug(logPrefix+"Connexion Oracle OK");
      } catch (ClassNotFoundException e) {
        String mess = "Erreur interne: classe non trouvee: "+e.getMessage();
        logger.fatal(logPrefix+mess);
        throw new GDMDBException(mess, e);
      } catch (SQLException e) {
        String mess = "Impossible de se connecter a la base de donnees: "+e.getMessage().replaceAll("\\n\\r?", "");
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
  
  public void traceQuery(String query) {
    if (isDebugSql()) {
      logger.info(query);
    }
  }
  
  public CallableStatement prepareCall(String query) throws SQLException
  {
    return getConnection().prepareCall(query);
  }
  
  @Override
  public boolean getBooleanProperty(String prop) throws NumberFormatException {
    String prop_text = getProperty(prop);
    if (prop_text==null || "".equals(prop_text)) {
      return false;
    }
    else {
      prop_text = prop_text.toLowerCase();
      if ("1".equals(prop_text) || "true".equals(prop_text)) {
        return true;
      }
      else if ("0".equals(prop_text) || "false".equals(prop_text)) {
        return false;
      }
      else {
        // Paramètre au format incorrect
        logger.error("Option "+prop+ " incorrecte: mettre true ou false ou 0 ou 1");
        throw new NumberFormatException();
      }
    }
  }
  
  @Override
  public boolean initialize(String[] args) throws ToolException, IOException {
    if (!super.initialize(args)) {
      return false;
    }
    
    // Autres initialisations selon configuration
    boolean initOk = true;
    
    try {
      debugSql = getBooleanProperty(propDebugSql);
    } catch (NumberFormatException e) {
      initOk = false;
    }
    
    try {
      oneShot = getBooleanProperty(propOneShot);
    } catch (NumberFormatException e) {
      initOk = false;
    }
    
    try {
      dureeAttente = Integer.parseInt(getProperty(propTempo)) * 1000;
    } catch (NumberFormatException e) {
      // Paramètre au format incorrect
      logger.error("Option "+propTempo+ " incorrecte, il faut un temps en secondes");
      initOk = false;
    }
        
    return initOk;
  }
  
  public boolean readGdmConfig (String configPath, boolean allInstances) {
    String oldConfigPath = configPath;
    try {
      readConfig(configPath, allInstances);
    } catch (IOException e) {
      logger.error("Erreur dans la lecture de la configuration "+configPath+": "+e.getMessage());
      return false;
    } finally {
      configPath = oldConfigPath;
    }
    return true;
  }
  
  // Cycle de traitement (pour les moteurs restant actifs)
  public void cycleTraitement () throws ToolException {
    
  }
  
  // Exécution de type boucle infinie
  public void executeBoucle(boolean catchExceptions) throws ToolException {
    
    boolean firstExecution = true;
    
    while (firstExecution || !oneShot) {
      firstExecution = false;
      
      try {
        // Connexion à Oracle
        connectDB(true);
  
        // Ewécution d'un cycle moteur
        cycleTraitement();
        
        // Fin du cycle
        disconnectDB();
      } catch (Exception e) {
        if (catchExceptions) {
          logger.error("Erreur: "+e.getMessage());
        }
        else {
          throw e;
        }
      }
      if (!oneShot) try {
        logger.info("Attente "+(dureeAttente/1000)+" s");
        Thread.sleep(dureeAttente);
      } catch (InterruptedException e) {
        break; // Sortie sur interruption
      }
    }
  }
  
  public static void main(String[] args) {
    try {
      main (GDMTool.class, GDMTool.classParameters, args);
    } catch (ToolException e) {
      e.printStackTrace();
    }
  }
}
