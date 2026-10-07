[![CI](https://github.com/sofypr/DAYS-Integracion_continua/actions/workflows/ci.yml/badge.svg)](https://github.com/sofypr/DAYS-Integracion_continua/actions/workflows/ci.yml)

# Taller de Integración Continua con GitHub Actions

Este taller tiene como objetivo aprender a **configurar, ejecutar e interpretar un pipeline de Integración Continua (CI)** que, en cada commit, compile el proyecto, ejecute las pruebas automatizadas y **bloquee el cambio si la calidad no cumple el umbral acordado**.
Es la base del pipeline que exige la **entrega 3 del proyecto del curso**, al que después se le agregan la construcción de la imagen Docker, los escaneos de seguridad y el despliegue.

---

## Objetivo General

Comprender, configurar y aplicar un **pipeline de Integración Continua** con **GitHub Actions** sobre un proyecto **Java + Maven**, integrando **pruebas unitarias (JUnit)**, **cobertura con umbral mínimo (JaCoCo, 80%)**, **publicación de reportes** y **protección de la rama principal**, de forma que ningún cambio que rompa el build llegue a `main`.

---

## Índice

- [Conceptos clave](#conceptos-clave)
- [CONOCE EL TALLER](#conoce-el-taller)
  - [Estructura del Proyecto](#estructura-del-proyecto)
  - [Configuración de Dependencias](#configuración-de-dependencias)
  - [El pipeline](#el-pipeline)
- [PASO A PASO](#paso-a-paso)
  - [Paso 1: Crea tu copia del repositorio](#paso-1-crea-tu-copia-del-repositorio)
  - [Paso 2: Primer pipeline en verde](#paso-2-primer-pipeline-en-verde)
  - [Paso 3: Pipeline en rojo con TDD](#paso-3-pipeline-en-rojo-con-tdd-prueba-primero)
  - [Paso 4: Pipeline en rojo por cobertura](#paso-4-pipeline-en-rojo-por-cobertura)
  - [Paso 5: Protege la rama main](#paso-5-protege-la-rama-main)
  - [Retos opcionales](#retos-opcionales)
- [Alternativa: GitLab CI](#alternativa-gitlab-ci)
- [Otros lenguajes](#otros-lenguajes)
- [Buenas prácticas](#buenas-prácticas)
- [Para entregar](#para-entregar-con-este-taller)
- [Cómo usar esta guía para tu proyecto](#cómo-usar-esta-guía-para-tu-proyecto)
- [Resumen del Taller](#hagamos-un-resumen)
- [Conclusión](#conclusión)
- [Recursos recomendados](#recursos-recomendados)
- [Créditos y uso académico](#créditos-y-uso-académico)
- [Licencia](#licencia-de-uso)

---

## Conceptos clave

- **Integración Continua (CI)**
Práctica en la que los desarrolladores integran sus cambios al repositorio compartido **con frecuencia** y cada integración se **verifica automáticamente** (build + pruebas).
Ejemplo: cada `git push` dispara un pipeline que compila y ejecuta todas las pruebas del proyecto.

- **Pipeline / workflow**
Secuencia automatizada de pasos que se ejecuta ante un evento (push, pull request). En GitHub Actions se define en un archivo YAML dentro de `.github/workflows/`.

- **Runner**
Máquina (virtual o contenedor) donde se ejecuta el pipeline. En GitHub, `ubuntu-latest` es una máquina **nueva y limpia en cada ejecución**: nada queda instalado de una corrida a otra.

- **Umbral de calidad (quality gate)**
Condición mínima que debe cumplir el código para que el pipeline pase. En este taller: **todas las pruebas en verde y cobertura de líneas ≥ 80%**.

- **Artefacto**
Archivo generado por el pipeline que se conserva para revisarlo después (por ejemplo, el reporte HTML de cobertura).

- **Rama protegida**
Regla del repositorio que impide integrar cambios a `main` si el pipeline no está en verde.

---

## CONOCE EL TALLER

La aplicación es intencionalmente pequeña: una **calculadora de la nota definitiva** con tres cortes (30% - 30% - 40%).
El foco del taller es el **pipeline**.

### Estructura del Proyecto

```text
.
├─ .github/workflows/ci.yml                      # el pipeline (GitHub Actions)
├─ pom.xml                                       # Maven + JUnit + JaCoCo (umbral de cobertura del 80%)
├─ src/main/java/edu/unisabana/dyas/notas/
│   └─ CalculadoraNotas.java                     # regla de negocio: definitiva, aprobación, validaciones
├─ src/test/java/edu/unisabana/dyas/notas/
│   └─ CalculadoraNotasTest.java                 # pruebas unitarias (JUnit, pruebas parametrizadas)
├─ gitlab/.gitlab-ci.yml                         # el mismo pipeline en GitLab CI (alternativa)
└─ otros-lenguajes/README.md                     # el pipeline para Node.js, Python y PHP
```

### Configuración de Dependencias

El [`pom.xml`](pom.xml) define tres piezas:

| Pieza | Para qué |
|---|---|
| `junit-jupiter` (vía `junit-bom`) | Pruebas unitarias, incluidas pruebas parametrizadas con `@CsvSource`. |
| `maven-surefire-plugin` | Ejecuta las pruebas en la fase `test` y genera reportes XML en `target/surefire-reports/`. |
| `jacoco-maven-plugin` | Mide la cobertura (`prepare-agent`), genera el reporte HTML (`report`) y **hace fallar el build** si la cobertura de líneas baja del 80% (`check`). |

El umbral está en una propiedad del `pom.xml`:

```xml
<jacoco.cobertura.minima>0.80</jacoco.cobertura.minima>
```

> El umbral vive en el **build**, no en el YAML del pipeline: así el resultado es el mismo en tu máquina y en el servidor de CI.

### El pipeline

Abre [`.github/workflows/ci.yml`](.github/workflows/ci.yml). Las piezas clave son:

| Bloque | Qué hace |
|---|---|
| `on: push` / `pull_request` | **Cuándo** corre: en cada push a cualquier rama y en cada Pull Request hacia `main`. `workflow_dispatch` permite lanzarlo a mano. |
| `permissions: contents: read` | Mínimo privilegio: el pipeline solo puede leer el código. |
| `runs-on: ubuntu-latest` | **Dónde** corre: una máquina virtual nueva en cada ejecución. |
| `actions/checkout@v7` | Descarga el código del commit que disparó el pipeline. |
| `actions/setup-java@v6` + `cache: maven` | Instala Java 17 y guarda las dependencias de Maven entre ejecuciones para que el build sea más rápido. |
| `mvn -B verify` | Compila, ejecuta las pruebas, genera el reporte de JaCoCo y **verifica el umbral de cobertura**. Si algo falla, el paso falla y el pipeline queda en rojo. |
| `Resumen de cobertura` | Escribe el porcentaje de cobertura en el resumen de la ejecución. |
| `actions/upload-artifact@v7` con `if: always()` | Publica los reportes de pruebas y cobertura **incluso si el build falló**, para poder revisar la causa del fallo. |

> **Sobre las versiones de las actions:** usa siempre la versión vigente de cada action. Las versiones viejas
> (`@v2`, `@v3`, `@v4`) corren sobre versiones de Node.js que GitHub ya retiró de sus runners.

---

## PASO A PASO

### Prerrequisitos

- Cuenta de GitHub.
- Git.
- Opcional, para correr las pruebas en tu máquina: **JDK 17 o superior** y **Maven 3.9**.

### Paso 1: Crea tu copia del repositorio

1. Haz **Fork** de este repositorio en tu cuenta de GitHub.
2. En tu fork, ve a la pestaña **Actions** y, si GitHub lo pide, habilita los workflows
   (en los forks vienen deshabilitados por defecto).
3. Clónalo en tu máquina:

   ```sh
   git clone https://github.com/<tu-usuario>/DAYS-Integracion_continua.git
   cd DAYS-Integracion_continua
   ```

4. (Opcional) Corre localmente lo mismo que va a correr el pipeline:

   ```sh
   mvn verify
   ```

   Debes ver `Tests run: 11, Failures: 0`, `All coverage checks have been met.` y `BUILD SUCCESS`.

### Paso 2: Primer pipeline en verde

1. Haz un cambio pequeño (por ejemplo, agrega los nombres de tu equipo en un archivo `integrantes.txt`), y luego commit y push:

   ```sh
   git add integrantes.txt
   git commit -m "Primer pipeline"
   git push
   ```

2. Ve a la pestaña **Actions** de tu fork y abre la ejecución. Revisa:
   - El log de cada paso (expande `Compilar, probar y verificar cobertura`).
   - El **resumen** con el porcentaje de cobertura.
   - El artefacto **reportes**: descárgalo y abre `target/site/jacoco/index.html`.

### Paso 3: Pipeline en rojo con TDD (prueba primero)

Nuevo requisito: *"Como estudiante, quiero saber qué nota necesito en el tercer corte para aprobar"*.

1. Crea `src/test/java/edu/unisabana/dyas/notas/NotaNecesariaTest.java` con esta prueba, **sin implementar nada todavía**:

   ```java
   package edu.unisabana.dyas.notas;

   import static org.junit.jupiter.api.Assertions.assertEquals;
   import static org.junit.jupiter.api.Assertions.assertThrows;

   import org.junit.jupiter.api.Test;
   import org.junit.jupiter.params.ParameterizedTest;
   import org.junit.jupiter.params.provider.CsvSource;

   class NotaNecesariaTest {

       private final CalculadoraNotas calculadora = new CalculadoraNotas();

       @ParameterizedTest(name = "{0}, {1} -> necesita {2}")
       @CsvSource({
               "3.0, 3.0, 3.0",
               "2.0, 2.0, 4.5",
               "3.5, 2.8, 2.8",
               "5.0, 5.0, 0.0"
       })
       void calculaLaNotaNecesariaEnElTercerCorte(double c1, double c2, double esperada) {
           assertEquals(esperada, calculadora.notaNecesariaTercerCorte(c1, c2));
       }

       @Test
       void avisaCuandoYaNoEsPosibleAprobar() {
           assertThrows(IllegalStateException.class, () -> calculadora.notaNecesariaTercerCorte(1.0, 1.0));
       }
   }
   ```

2. Haz commit y push **solo de la prueba**. El pipeline queda en **rojo** porque la prueba ni siquiera compila
   (`cannot find symbol`). Esa es la fase **Red** de TDD, ahora visible para todo el equipo.

   > Variante: si primero agregas el método con un cuerpo vacío (`return 0;`), el pipeline queda en rojo por
   > **pruebas fallidas** en lugar de por compilación. Compara los dos logs.

3. Implementa `notaNecesariaTercerCorte(double corte1, double corte2)` en `CalculadoraNotas`:
   - Nota necesaria = (3.0 − 0.3·corte1 − 0.3·corte2) / 0.4, redondeada **hacia arriba** a una décima.
   - Si es mayor que 5.0, lanza `IllegalStateException` (ya no es posible aprobar).
   - Si es menor que 0.0, devuelve 0.0.
   - Las notas de entrada se validan igual que en `calcularDefinitiva`.

4. Commit y push. El pipeline vuelve a **verde** (fase **Green**).

### Paso 4: Pipeline en rojo por cobertura

1. Agrega a `CalculadoraNotas` un método **sin escribirle pruebas**, por ejemplo:

   ```java
   public String concepto(double definitiva) {
       BigDecimal nota = validar(definitiva, "definitiva");
       if (nota.compareTo(new BigDecimal("4.5")) >= 0) {
           return "Excelente";
       } else if (nota.compareTo(new BigDecimal("4.0")) >= 0) {
           return "Sobresaliente";
       } else if (nota.compareTo(NOTA_APROBATORIA) >= 0) {
           return "Aprobado";
       }
       return "Reprobado";
   }
   ```

2. Commit y push. **Todas las pruebas pasan, pero el pipeline queda en rojo**:

   ```text
   Rule violated for bundle calculadora-notas: lines covered ratio is 0.74, but expected minimum is 0.80
   ```

3. Escribe las pruebas que faltan hasta que el pipeline vuelva a verde.

> **Para discutir:** ¿qué garantiza un 80% de cobertura y qué **no** garantiza? ¿Se podría llegar al 80% con pruebas
> que no verifican nada (sin `assert`)?

### Paso 5: Protege la rama main

Configura GitHub para que **impida** integrar código que rompa el build:

1. En tu fork: **Settings → Rules → Rulesets → New ruleset → New branch ruleset**.
2. Nombre: `proteger-main`. *Enforcement status*: **Active**. *Target branches*: **Include default branch**.
3. Activa **Require a pull request before merging** y **Require status checks to pass**; agrega el check
   `Build, pruebas y cobertura` (solo aparece en la lista si el pipeline ya corrió al menos una vez en el repositorio).
4. Guarda. Crea una rama, rompe una prueba, abre un Pull Request hacia `main` y comprueba que el botón de *merge*
   queda bloqueado.

### Retos opcionales

1. **Matriz de versiones:** usa `strategy.matrix` para probar con Java 17 y Java 21 en paralelo.
2. **Badge:** agrega al README la insignia del estado del pipeline
   (`https://github.com/<usuario>/<repo>/actions/workflows/ci.yml/badge.svg`).
3. **GitLab CI:** sigue la guía de la sección siguiente y compara la sintaxis con la de GitHub Actions.

---

## Alternativa: GitLab CI

El archivo [`gitlab/.gitlab-ci.yml`](gitlab/.gitlab-ci.yml) implementa el mismo pipeline en GitLab:

1. Crea un proyecto en GitLab e importa este repositorio (**New project → Import project → Repository by URL**).
2. Copia `gitlab/.gitlab-ci.yml` a la **raíz** del repositorio (GitLab solo lee ese archivo desde la raíz).
3. Commit y push, y revisa **Build → Pipelines**.

> Declara siempre `image:`. Sin esa línea, en los runners compartidos de GitLab.com el job corre en una imagen
> por defecto (`ruby`) que no tiene Java ni Maven.

Equivalencias principales:

| GitHub Actions | GitLab CI |
|---|---|
| `.github/workflows/*.yml` | `.gitlab-ci.yml` en la raíz |
| `jobs` + `needs` | `stages` + jobs |
| `runs-on: ubuntu-latest` + `setup-java` | `image: maven:3.9-eclipse-temurin-17` |
| `cache: maven` | `cache:` |
| `actions/upload-artifact` | `artifacts:` |

---

## Otros lenguajes

Si el proyecto de tu equipo no es Java, revisa [`otros-lenguajes/README.md`](otros-lenguajes/README.md):
tiene el mismo pipeline para **Node.js (Jest/Mocha)**, **Python (PyTest)** y **PHP (PHPUnit)**, cada uno con su umbral de cobertura.

---

## Buenas prácticas

1. **Commits pequeños y frecuentes:** entre más pequeño el cambio, más fácil saber qué rompió el pipeline.
2. **El pipeline en rojo se arregla primero:** nadie integra nada nuevo mientras `main` esté rota.
3. **Mismo comando local y en CI:** si `mvn verify` pasa en tu máquina, debe pasar en el pipeline (y viceversa).
4. **Umbrales en el build, no en la cabeza:** la cobertura mínima se verifica automáticamente, no se revisa “a ojo”.
5. **Reportes siempre disponibles:** publica los reportes también cuando el build falla.
6. **Mínimo privilegio:** el pipeline solo con los permisos que necesita (`contents: read`).
7. **Versiones vigentes:** actions, lenguajes e imágenes con soporte; las versiones obsoletas dejan de funcionar sin aviso.

---

## PARA ENTREGAR CON ESTE TALLER

### 1) Repositorio

- **Fork** de este repositorio con **URL pública o acceso por invitación**.
- Archivo **`integrantes.txt`** o sección en el README con nombres y correos institucionales.
- **Rama principal ejecutable:** `mvn verify` en verde, sin configuraciones manuales adicionales.

### 2) Documentación en Wiki (obligatoria)

> Toda la documentación del taller se entrega en el **Wiki del repositorio**.
> No se requiere PDF; el Wiki es la entrega oficial.

Estructura mínima sugerida del Wiki:

- **Inicio:** integrantes y propósito del taller.
- **El pipeline:** explicación, con sus palabras, de cada bloque del `ci.yml`.
- **Evidencias:** capturas de las ejecuciones en verde y en rojo (pasos 2, 3 y 4) con el log del error.
- **Cobertura:** captura del reporte JaCoCo descargado del artefacto.
- **Rama protegida:** captura del Pull Request bloqueado.
- **Reflexión final.**

### 3) Pipeline de CI

- `.github/workflows/ci.yml` ejecutándose en cada push y en cada Pull Request.
- Al menos **una ejecución en verde** visible en la pestaña **Actions**.

### 4) TDD en el pipeline

- Commit con **solo la prueba** (`NotaNecesariaTest`) y su ejecución en **rojo**.
- Commit con la **implementación** y su ejecución en **verde**.

### 5) Umbral de cobertura

- Ejecución en **rojo por cobertura** (paso 4) con el mensaje de JaCoCo.
- Ejecución posterior en verde con las pruebas que faltaban.

### 6) Rama protegida

- Ruleset sobre `main` que exige Pull Request y el check `Build, pruebas y cobertura`.
- Un Pull Request **bloqueado** por un pipeline en rojo.

### 7) Reflexión final (en el Wiki)

- ¿Qué diferencia hay entre un pipeline en rojo por compilación, por pruebas fallidas y por cobertura?
- ¿Qué garantiza y qué **no** garantiza un 80% de cobertura?
- ¿Por qué el umbral de cobertura se define en el `pom.xml` y no en el YAML?
- ¿Qué tendrían que agregarle a este pipeline para cumplir la entrega 3 del proyecto?

### 8) Rúbrica – Taller de Integración Continua

| **Criterios de evaluación** | **Indicadores de cumplimiento** | **Excelente (5 pts)** | **Bueno (4 pts)** | **Necesita mejorar (3.5 pts)** | **Deficiente (2.5 pts)** | **No cumple (0 pts)** |
|-----------------------------|----------------------------------|------------------------|-------------------|-------------------------------|--------------------------|------------------------|
| **Repositorio** | Fork organizado, con integrantes y rama principal ejecutable. | Repositorio ordenado, `mvn verify` en verde, integrantes registrados. | Ejecutable con detalles menores de organización. | Requiere ajustes para ejecutar. | Errores de compilación o estructura desordenada. | No entrega o el código no ejecuta. |
| **Pipeline de CI** | Workflow que corre en push y Pull Request, con reportes publicados. | Pipeline en verde, dispara en push y PR, publica reportes y resumen de cobertura. | Pipeline en verde con algún elemento faltante (reportes o PR). | Pipeline configurado pero con ejecuciones fallidas sin explicar. | Workflow presente pero nunca se ejecutó correctamente. | No hay pipeline. |
| **TDD en el pipeline** | Evidencia del ciclo Red → Green en el historial. | Commit rojo (solo prueba) y commit verde (implementación), con logs explicados. | Ciclo completo sin análisis de los logs. | Solo una de las dos fases evidenciada. | Implementación sin prueba previa. | No realiza el ejercicio. |
| **Umbral de cobertura** | Uso del umbral del 80% como criterio de calidad. | Ejecución roja por cobertura y recuperación a verde con pruebas significativas. | Recupera el verde, pero con pruebas débiles (sin aserciones relevantes). | Solo evidencia la ejecución en rojo. | Baja o elimina el umbral para pasar. | No realiza el ejercicio. |
| **Rama protegida** | Ruleset sobre `main` con check obligatorio. | Ruleset activo y Pull Request bloqueado evidenciado. | Ruleset activo sin evidencia del bloqueo. | Ruleset sin el check de CI. | Ruleset inactivo o mal configurado. | No configura la protección. |
| **Documentación en Wiki** | Secciones completas con evidencias y explicación del pipeline. | Wiki completo, claro y con capturas de todas las ejecuciones. | Wiki completo con leves omisiones. | Wiki incompleto o con poca claridad. | Wiki muy limitado o confuso. | No hay Wiki o está vacío. |
| **Reflexión técnica** | Análisis de resultados y aprendizajes. | Reflexión profunda, conectada con la entrega 3 del proyecto. | Reflexión correcta pero superficial. | Reflexión breve o poco argumentada. | Reflexión vaga o sin relación con el taller. | No presenta reflexión. |

> **Cómo suma**: 7 criterios × 5 pts = **35 puntos**.

| Rango de puntaje | Desempeño                                                |
| ---------------- | -------------------------------------------------------- |
| 31 – 35          | Excelente dominio técnico y metodológico.                |
| 25 – 30          | Buen trabajo con evidencias o documentación parcial.     |
| 21 – 24          | Cumple con lo básico pero sin profundidad.               |
| < 21             | No cumple con los criterios mínimos del taller.          |

---

## Propósito del taller

En este taller se configura un pipeline de CI para el equipo: cada commit se compila y se prueba automáticamente, la cobertura mínima se verifica sin intervención manual y la rama principal queda protegida.

A través del caso `CalculadoraNotas` se conectan las prácticas ya vistas en el curso (**TDD**, **pruebas unitarias**, **calidad del código**) con la automatización que exige un proceso **DevOps**, preparando el terreno para contenerizar, asegurar y desplegar el proyecto.

---

## Cómo usar esta guía para tu proyecto

1. **Copia el workflow** a `.github/workflows/ci.yml` del repositorio del proyecto y ajusta el paso de instalación a tu lenguaje (ver [`otros-lenguajes`](otros-lenguajes/README.md)).
2. **Configura el umbral de cobertura** en tu herramienta de build (JaCoCo, `--cov-fail-under`, `coverageThreshold`…). El enunciado del Proyecto 3 exige **al menos 70 % de líneas en el dominio**; el equipo puede definir un umbral mayor.
3. **Publica los reportes** de pruebas y cobertura como artefactos: son evidencia para el informe final.
4. **Protege `main`** con un ruleset que exija Pull Request, la aprobación de otro integrante y el pipeline en verde.
5. **Trabaja con Pull Requests**: cada integrante integra sus cambios mediante PR revisado y con CI en verde.

Checklist para el Proyecto 3 (sección 2 del enunciado):

- [ ] Un workflow que corra **en cada push y en cada Pull Request**.
- [ ] Build, pruebas unitarias y pruebas de integración del Corte 2 ejecutándose en el pipeline.
- [ ] Umbral de cobertura del dominio de **al menos 70 %** que haga fallar el build.
- [ ] Reportes de pruebas y cobertura publicados como artefactos.
- [ ] Caché de dependencias (el pipeline de integración debe tardar menos de 10 minutos).
- [ ] La rama `main` protegida: Pull Request obligatorio, revisión aprobada por otro integrante y checks en verde.
- [ ] Badge de estado del pipeline en el README.

---

> **Resultado esperado:**
> Al finalizar este taller, cada equipo contará con un **pipeline de CI funcionando en el repositorio de su proyecto**, que ejecuta las pruebas en cada commit, exige una **cobertura mínima** que rompe el build y protege la rama principal, listo para incorporar la **contenerización (Docker)**, los **escaneos de seguridad (DevSecOps)** y el **despliegue continuo (CD)**.

---

## Hagamos un resumen

### Integración Continua

- **Qué es:** integrar cambios con frecuencia y **verificar cada integración automáticamente**.
- **Para qué sirve:** detectar errores **minutos** después de introducirlos, no semanas después, cuando ya es caro corregirlos.
- **Ejemplo típico:** cada `git push` dispara `mvn verify` en un runner limpio.

### Umbral de cobertura

- **Qué es:** un porcentaje mínimo de código ejecutado por las pruebas, verificado por el build.
- **Para qué sirve:** evitar que el código nuevo entre sin pruebas.
- **Limitación:** mide qué código se **ejecutó**, no qué se **verificó**; una prueba sin aserciones también suma cobertura.

### Rama protegida

- **Qué es:** una regla del repositorio que exige Pull Request y pipeline en verde para integrar a `main`.
- **Para qué sirve:** garantiza que ningún cambio llegue a `main` sin pasar el pipeline.

### GitHub Actions vs. GitLab CI

- **Mismo concepto, distinta sintaxis:** eventos que disparan el pipeline, jobs que corren en un entorno limpio, caché, artefactos y reportes.

---

## Conclusión

En conjunto, estas prácticas permiten:

- Verificar automáticamente cada cambio (**pipeline de CI**).
- Hacer visible el ciclo **Red → Green** de TDD para todo el equipo.
- Mantener un mínimo de calidad sin depender de la revisión manual (**umbral de cobertura**).
- Impedir que un cambio defectuoso llegue a la rama principal (**rama protegida**).

Con esto se logra **retroalimentación rápida**, **una rama principal siempre funcional** y **evidencia automática de la calidad del software**, que es la base para la entrega continua y el despliegue continuo.

---

## Recursos recomendados

- *Continuous Integration: Improving Software Quality and Reducing Risk* – Paul M. Duvall
- *Continuous Delivery* – Jez Humble & David Farley
- Martin Fowler – [Continuous Integration](https://martinfowler.com/articles/continuousIntegration.html)
- GitHub Docs – [Building and testing Java with Maven](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven)
- GitHub Docs – [Workflow syntax for GitHub Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- GitHub Docs – [About rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
- [JaCoCo – check goal](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html)
- GitLab Docs – [CI/CD YAML syntax reference](https://docs.gitlab.com/ci/yaml/)

---

## Créditos y uso académico

**Autor:** César Augusto Vega Fernández
**Curso:** Diseño y Arquitectura de Software
**Programa:** Ingeniería Informática – Universidad de La Sabana
**Año:** 2026

Este taller y su contenido fueron diseñados por el profesor **César Augusto Vega Fernández** como material académico para el curso *Diseño y Arquitectura de Software*, impartido en el programa de **Ingeniería Informática de la Universidad de La Sabana**.

Su propósito es exclusivamente educativo y está orientado a fortalecer las competencias de los estudiantes en **Integración Continua, automatización de pruebas, umbrales de calidad** y prácticas **DevOps** aplicadas al desarrollo de software.

---

### Licencia de uso

Este material se distribuye bajo la licencia [Creative Commons Atribución-NoComercial-CompartirIgual 4.0 Internacional (CC BY-NC-SA 4.0)](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.es).

Puedes **usar, adaptar o compartir** este contenido con fines educativos, siempre que:

1. Se reconozca la autoría del profesor **César Augusto Vega Fernández**.
2. No se utilice con fines comerciales.
3. Las obras derivadas se distribuyan bajo la misma licencia.

---

© Universidad de La Sabana – Facultad de Ingeniería
Ingeniería Informática – 2026
