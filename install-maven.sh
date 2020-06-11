# Installation dans le repository maven local
#
#  Certaines versions des drivers ojdbc ne sont plus disponibles, notamment celle pour
#  Oracle 10. Ce script installe les jars conservés dans jdbc/ dans un dépôt local
#  au projet. L'utilisation de ce dépot se fait en mettant dans pom.xml:
#
#  <repositories>
#    <repository>
#        <id>local-maven-repo</id>
#        <url>file:///${project.basedir}/local-maven-repo</url>
#    </repository>
#  </repositories>
#
# Nb: le numero qui suit ojdbc est la version Java. La version d'Oracle est après le tiret.
install_local_repo() {
  file=$1
  group=$2
  arti=$3
  vers=$4
  mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file  \
    -Dfile="$file" -DgroupId=$group -DartifactId=$arti -Dversion=$vers \
    -Dpackaging=jar -DgeneratePom=true \
    -DlocalRepositoryPath=local-maven-repo
}

install_local_repo jdbc/ojdbc14-10.2.0.1.0.jar com.oracle ojdbc14 10.2.0.1.0
install_local_repo jdbc/ojdbc6-11.2.0.4.jar com.oracle ojdbc6 11.2.0.4
install_local_repo jdbc/ojdbc6-12.1.0.1.jar com.oracle ojdbc6 12.1.0.1


