# ---------- Etapa 1: build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cachear dependencias antes de copiar el código: si solo cambias src/,
# esta capa no se invalida y el build es mucho más rápido.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Usuario sin privilegios: Render no lo exige, pero evita ejecutar como root.
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /app/target/appPDFtoMD-0.0.1-SNAPSHOT.jar app.jar
USER app

EXPOSE 63919

# -Xmx350m calibrado para el plan gratuito de Render (512 MB).
# En máquina propia puedes subir el valor: -Xmx2g o más.
ENTRYPOINT ["java", "-Xmx350m", "-jar", "/app/app.jar"]