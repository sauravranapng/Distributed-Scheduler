# ---------- Build Stage ----------
# ---Start building my image from an existing image that already contains Java 22 , linux file system and Maven 3.9.9 .
#You require mvn to compile and package your Java application becuase java -c does not compile and package the entire application.
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Creates (if necessary) and switches into /app directory in the container.
# All subsequent commands will be run from this directory of container. because of this COPY pom.xml . -> this . will be /app of linux file system of the container instead of
#the current directory of the host machine.
WORKDIR /app

# Why copying pom.xml first and then running mvn dependency:go-offline instead of copying the whole project?
# Because Docker caches individual command layers during the image build process.
# If an input has changed, that cache breaks, and Docker must re-run that command and every single command after it.
# If you change your source code but not your dependencies, Docker will use the cached layer and skip downloading dependencies again,which speeds up the build process.
COPY pom.xml .

# Downloads all dependencies from Maven Central.
# And caches them in a Docker's Maven cache.
RUN mvn dependency:go-offline

# Copies your Java source code to app/src directory of the container.
COPY src ./src

#Packages into Jar wihtout running tests to speed up the build process.
RUN mvn clean package -DskipTests

# ---------- Runtime Stage ---------- This will creeate a runtime Image that contains only the compiled application(app.jar) and the Java 22 runtime environment.

# we don not have maven here because we do not need it to run the application. We only need Java 21 to run the app.jar.
FROM eclipse-temurin:21-jre

WORKDIR /app

#Copy the jar from above build stage to the current working directory of this runtime image.
COPY --from=build /app/target/*.jar app.jar

# Runtime Image has NO SOURCE CODE ,NO MAVEN ,NO TARGET DIRECTORY ,This is called a multi-stage build

#EXPOSE is mainly documentation and helps other tools understand which port the application uses inside container.
#It does not actually publish the port. You still need to use -p option with docker run to publish the port.
EXPOSE 8084

#This is the command Docker runs when the container starts.I can use Java options like -Xmx to set the maximum heap size for the JVM.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]