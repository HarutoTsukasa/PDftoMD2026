# appPDFtoMD — Convertidor local a Markdown

Aplicación web local construida con Spring Boot que recibe archivos **PDF, DOCX, XLSX/XLS y PPTX** y devuelve su contenido convertido a **Markdown**. La conversión se ejecuta íntegramente en la máquina del usuario: sin cuentas, sin servicios en la nube y sin almacenamiento remoto.

---

## Características

- **Landing page** con arrastrar y soltar y selector de archivo tradicional.
- **Conversión en memoria** de PDF, DOCX, XLSX/XLS y PPTX a Markdown.
- **Descarga del resultado** como archivo `.md`.
- **Vista previa** del Markdown en un área de texto con contador de líneas y palabras, y botón de copia al portapapeles.
- **Tema claro / oscuro** con preferencia guardada en `localStorage`.
- **Manejo de errores** visible en la interfaz: archivo vacío, formato no soportado o fallo de conversión.
- **Procesamiento local**: no se envía nada a Internet durante la conversión.

---

## Stack y dependencias

Proyecto generado con Spring Boot **4.1.1**, Java **25**, Maven Wrapper **3.3.4** (distribución Maven **3.10.0**).

Dependencias declaradas en `pom.xml`:

| Grupo : Artefacto | Versión | Uso |
|---|---|---|
| `org.springframework.boot : spring-boot-starter-parent` | 4.1.1 | Parent y gestión de versiones |
| `org.springframework.boot : spring-boot-starter-thymeleaf` | (gestionada) | Renderizado del landing |
| `org.springframework.boot : spring-boot-starter-webmvc` | (gestionada) | Spring MVC y Tomcat embebido |
| `org.springframework.boot : spring-boot-starter-thymeleaf-test` | (gestionada) | Tests de Thymeleaf |
| `org.springframework.boot : spring-boot-starter-webmvc-test` | (gestionada) | Tests de Spring MVC |
| `org.apache.pdfbox : pdfbox` | 3.0.3 | Extracción de texto de PDF |
| `org.docx4j : docx4j-core` | 11.5.3 | Lectura de DOCX |
| `org.docx4j : docx4j-JAXB-ReferenceImpl` | 11.5.3 | Runtime JAXB para docx4j |
| `com.vladsch.flexmark : flexmark-html2md-converter` | 0.64.8 | Conversión HTML → Markdown |
| `org.apache.poi : poi-ooxml` | 5.3.0 | XLSX, XLS y PPTX |
| `org.apache.commons : commons-lang3` | (gestionada) | Utilidades |

---

## Requisitos

- **JDK 25** (el `pom.xml` fija `java.version=25`).
- No es necesario instalar Maven ni Java aparte del JDK: el wrapper `mvnw` / `mvnw.cmd` descarga Maven 3.10.0 en la primera ejecución.
- **Conexión a Internet** para la primera compilación (descarga de dependencias y Maven) y para cargar Bootstrap, FontAwesome y Google Fonts desde CDN en el navegador. La conversión de archivos es local.

---

## Compilación y ejecución

### Linux / macOS

```bash
./mvnw clean package
java -Xmx2g -jar target/appPDFtoMD-0.0.1-SNAPSHOT.jar
```

### Windows

```bat
mvnw.cmd clean package
java -Xmx2g -jar target\appPDFtoMD-0.0.1-SNAPSHOT.jar
```

Abre el navegador en:

```
http://localhost:63919
```

> El puerto y el tamaño máximo de archivo se configuran en `src/main/resources/application.properties`. El valor `-Xmx2g` es una recomendación de arranque; ajústalo según el tamaño de archivo que vayas a procesar.

---

## Configuración

`src/main/resources/application.properties`:

```properties
spring.application.name=appPDFtoMD

spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=105MB
server.port=63919
```

- **`max-file-size`** y **`max-request-size`**: límites de Spring Boot para la carga multipart. Son independientes del consumo real de memoria de cada biblioteca.
- **`server.port`**: puerto del servidor embebido (63919 en la configuración actual).

---

## Uso

1. Abre `http://localhost:63919`.
2. Arrastra un archivo sobre la zona de carga o haz clic para seleccionarlo. La interfaz muestra el nombre y tamaño del archivo.
3. Pulsa **Convertir a Markdown**.
4. Revisa el resultado en el área de texto. Puedes copiarlo con el botón **Copiar** o descargarlo como `.md` con **Descargar .md**.
5. Si el archivo no se puede procesar, la interfaz muestra un mensaje de error (archivo vacío, formato no soportado o fallo durante la conversión).

Formatos aceptados por el formulario: `.pdf`, `.docx`, `.xlsx`, `.pptx`.

---

## Formatos soportados y comportamiento

### PDF

- Extracción de **texto plano** con `PDFTextStripper` (PDFBox 3).
- El documento completo se carga en memoria (`in.readAllBytes()` + `Loader.loadPDF(byte[])`).
- Se antepone al resultado la cabecera `# Texto extraído del PDF`.

### DOCX

- Carga con `WordprocessingMLPackage.load(InputStream)`.
- Conversión a HTML con `Docx4J.toHTML(HTMLSettings, OutputStream, Docx4J.FLAG_EXPORT_PREFER_XSL)`.
- El HTML se convierte a Markdown con `FlexmarkHtmlConverter`.
- El método `convertDocx` está anotado con `@SuppressWarnings("deprecation")` porque `HTMLSettings.setWmlPackage` está marcado como obsoleto en docx4j 11.5.3.

### XLSX / XLS

- Carga con `WorkbookFactory.create(InputStream)` de Apache POI, que admite tanto `.xlsx` como `.xls`.
- Cada hoja se convierte en un bloque Markdown con su nombre como encabezado `##`.
- La primera fila de la tabla usa encabezados genéricos `Col1`, `Col2`, …
- Los valores se formatean con `DataFormatter`.
- El carácter `|` en el contenido se escapa como `\|` y los saltos de línea se sustituyen por espacios.

> El formulario del landing solo acepta `.xlsx` como extensión de Excel. El switch de `ConversionService` reconoce además `.xls`, pero este no es seleccionable desde la interfaz.

### PPTX

- Carga con `XMLSlideShow`.
- Por cada diapositiva se genera un bloque `## Diapositiva N` y se concatenan los textos de las formas que implementan `XSLFTextShape`.
- Cada diapositiva termina con un separador `---`.

---

## Limitaciones

- **Sin límite artificial de páginas, hojas o diapositivas.** El único techo real es la memoria del montón de la JVM (`-Xmx`) y el tiempo de CPU. Los archivos grandes (PDF de cientos de páginas, libros Excel con muchas filas) consumen memoria proporcional a su tamaño.
- **PDF:** solo se extrae texto plano. No se preservan encabezados, tablas, columnas, imágenes ni estructura. Los PDF escaneados o sin capa de texto utilizable devuelven una salida vacía o casi vacía. La aplicación no aplica OCR.
- **PDF:** `Loader.loadPDF` requiere un `byte[]` en esta versión de PDFBox; el documento entero se materializa en memoria antes de procesarse.
- **DOCX:** las imágenes pueden externalizarse u omitirse en el HTML intermedio. Tablas complejas o celdas combinadas pueden aplanarse durante la conversión a Markdown.
- **XLSX / XLS:** las celdas combinadas se expanden como celdas individuales. Los formatos condicionales, comentarios y fórmulas no se exportan como tales; se usa el valor formateado por `DataFormatter`. Libros con cientos de miles de filas agotan el heap con facilidad.
- **PPTX:** solo se extraen las formas de texto. Tablas nativas, SmartArt, gráficos e imágenes se ignoran o se exportan como texto suelto sin estructura.
- **`.doc` y `.ppt` heredados** no están soportados. Solo `.docx` y `.pptx`.
- **CDN en el landing:** la interfaz carga Bootstrap, FontAwesome y Google Fonts desde CDN. Sin conexión a Internet, el landing se muestra sin estilos; la conversión de archivos sigue siendo local.
- **Diagnóstico en consola:** el método `convertPdf` incluye código marcado como `// temporal` que imprime en `System.out` el número de páginas, las fuentes y XObjects de la primera página, y el número de páginas con texto extraíble. Es útil para diagnosticar PDFs problemáticos y puede retirarse cuando ya no sea necesario.

---

## Estructura del proyecto

```
appPDFtoMD/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/htx/app/pdftomd/
    │   │   ├── AppPdFtoMdApplication.java
    │   │   ├── controller/
    │   │   │   └── ConversionController.java
    │   │   └── service/
    │   │       └── ConversionService.java
    │   └── resources/
    │       ├── application.properties
    │       └── templates/
    │           └── index.html
    └── test/
        └── java/com/htx/app/pdftomd/
            └── AppPdFtoMdApplicationTests.java
```

---

## Notas técnicas

- `ConversionController` expone dos endpoints:
  - `POST /convert` — recibe un `MultipartFile` en el campo `file`, ejecuta la conversión y devuelve la vista `index` con los atributos `markdown` (o `error`) y `filename`.
  - `POST /download` — recibe `markdown` y `filename` como parámetros de formulario y devuelve el contenido como `text/markdown` con cabecera `Content-Disposition: attachment`.
- `ConversionService` selecciona la estrategia por extensión del archivo (`pdf`, `docx`, `xlsx`/`xls`, `pptx`). Una extensión distinta produce `IllegalArgumentException` con el mensaje `Formato no soportado: .<ext>. Use PDF, DOCX, XLSX o PPTX.`
- Toda la conversión ocurre en memoria. La aplicación no escribe archivos en disco ni persiste nada entre peticiones.
- El landing está construido con Bootstrap 5.3.3 (CDN), FontAwesome 6.5.1 (CDN) y Google Fonts (Inter y JetBrains Mono). El tema se guarda en `localStorage` con la clave `theme`.