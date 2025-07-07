# W Prime Extension para Hammerhead Karoo

Una extensión para Hammerhead Karoo basada en el nuevo framework **karoo-ext** que implementará el modelo de W Prime (W') para el seguimiento de la energía anaeróbica durante el entrenamiento y las carreras.

## Estado Actual del Proyecto

### ✅ En Desarrollo Avanzado (Configuración e Integración Completa)

Este proyecto ha integrado exitosamente la configuración persistente de W Prime con el cálculo en tiempo real:

- **✅ Base del proyecto**: Template oficial karoo-ext funcional
- **✅ Estructura de extensión**: `WPrimeExtension` heredando de `KarooExtension`
- **✅ Campo de datos W Prime**: `WPrimeDataType` calculando W Prime en tiempo real
- **✅ Configuración persistente**: DataStore integrado para CP, W' y Tau
- **✅ Interfaz de configuración**: UI completa con ConfigurationScreen y ViewModel
- **✅ Cálculo matemático**: WPrimeCalculator implementado con modelo completo
- **✅ Integración completa**: Configuración persistente vinculada con cálculo en tiempo real
- **✅ Compilación exitosa**: APK generado exitosamente (WPrimeExtension-v1.0-debug.apk)
- **✅ Sistema de logging unificado**: Implementado sistema estructurado para debugging (ver [LOGGING.md](LOGGING.md))
- **🔄 En progreso**: Pruebas en dispositivo y validación
- **⏳ Pendiente**: RemoteViews para visualización personalizada
- **⏳ Pendiente**: Integración con archivos FIT
- **⏳ Pendiente**: Optimización y ajustes basados en pruebas

## ¿Qué es W Prime (W')?

W Prime (W') es un modelo fisiológico que cuantifica la capacidad de trabajo anaeróbico de un ciclista:

- **Potencia Crítica (CP)**: El máximo esfuerzo sostenible teóricamente indefinido
- **W Prime (W')**: La cantidad finita de trabajo que se puede realizar por encima de CP
- **Recuperación**: W' se recupera exponencialmente cuando la potencia está por debajo de CP

### 🧮 Modelo Matemático a Implementar

1. **Depleción**: Cuando potencia > CP
   ```
   W'(t) = W'(t-1) - (Potencia - CP) × ΔTiempo
   ```

2. **Recuperación**: Cuando potencia < CP
   ```
   W'(t) = W'(t-1) + (W'máx - W'(t-1)) × (1 - e^(-ΔTiempo/Tau))
   ```

3. **Equilibrio**: Cuando potencia = CP
   ```
   W'(t) = W'(t-1) (sin cambio)
   ```

## Framework Karoo Extensions

Este proyecto utiliza el **nuevo framework karoo-ext** (no el SDK deprecado), que ofrece:

- **Proceso separado**: Las extensiones corren en su propio proceso para mayor estabilidad
- **API clara**: Interacción mediante eventos y efectos serializables
- **Data Types**: Sistema para crear campos de datos personalizados
- **RemoteViews**: Para vistas personalizadas seguras entre procesos
- **Integración moderna**: Compatible con Jetpack Compose y arquitecturas modernas

## Instalación y Desarrollo

### Requisitos

1. **Android Studio** con Kotlin support
2. **Java 8+** para la compilación
3. **Acceso a GitHub Packages** para karoo-ext dependency
4. **Hammerhead Karoo** device para testing

### Configuración Inicial

1. Clona el repositorio:
   ```bash
   git clone <tu-repo>
   cd WPrimeExtension
   ```

2. Configura credenciales para GitHub Packages en `local.properties`:
   ```
   gpr.user=tu-usuario-github
   gpr.key=tu-token-github
   ```

3. Compila el proyecto:
   ```bash
   ./gradlew assembleDebug
   ```

4. Instala en tu Karoo:
   ```bash
   adb install app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk
   ```

## Estructura del Proyecto Actual

```
WPrimeExtension/
├── app/                                    # Aplicación Android principal
│   ├── src/main/kotlin/com/itl/wprimeext/
│   │   ├── MainActivity.kt                 # Activity principal (del template)
│   │   ├── MainViewModel.kt               # ViewModel principal (del template)
│   │   ├── TabLayout.kt                   # UI layout (del template)
│   │   ├── ConfigurationScreen.kt         # ✅ Pantalla de configuración W Prime
│   │   ├── ui/
│   │   │   ├── viewmodel/
│   │   │   │   └── WPrimeConfigViewModel.kt  # ✅ ViewModel para configuración
│   │   │   └── components/
│   │   │       └── ConfigurationCard.kt   # ✅ Componente UI para parámetros
│   │   └── extension/
│   │       ├── WPrimeExtension.kt         # ✅ Extensión principal (completa)
│   │       ├── WPrimeDataType.kt          # ✅ Campo de datos W Prime (integrado)
│   │       ├── WPrimeCalculator.kt        # ✅ Motor de cálculo (implementado)
│   │       └── WPrimeSettings.kt          # ✅ Configuración con DataStore
│   ├── utils/                             # ✅ Utilidades del proyecto
│   │   ├── WPrimeLogger.kt               # ✅ Sistema de logging unificado
│   │   └── LogConstants.kt               # ✅ Constantes para logging
│   ├── src/main/res/xml/
│   │   └── extension_info.xml             # Definición de la extensión
│   └── manifest.json                      # Metadatos de la app
├── lib/                                   # Librería karoo-ext (código fuente)
├── build.gradle.kts                      # Configuración de build
├── LOGGING.md                            # ✅ Documentación del sistema de logging
└── README.md                             # Este archivo
```

### Archivos Clave Implementados

- **`WPrimeExtension.kt`**: ✅ Clase principal que hereda de `KarooExtension`
- **`WPrimeDataType.kt`**: ✅ Implementa `DataTypeImpl` con cálculo integrado
- **`WPrimeCalculator.kt`**: ✅ Algoritmo completo de W Prime con depleción/recuperación
- **`WPrimeSettings.kt`**: ✅ Configuración persistente usando Android DataStore
- **`ConfigurationScreen.kt`**: ✅ UI para configurar CP, W' y Tau
- **`WPrimeConfigViewModel.kt`**: ✅ ViewModel que conecta UI con configuración persistente
- **`extension_info.xml`**: Define los data types disponibles para Karoo OS
- **`manifest.json`**: Metadatos para instalación via Karoo Companion App

## Tecnologías y Dependencias

### Framework Principal
- **Hammerhead karoo-ext 1.1.5** - Framework oficial para extensiones
- **Kotlin** - Lenguaje principal
- **Android API Level 23-35** - Compatibilidad con Karoo devices

### UI y Arquitectura
- **Jetpack Compose** - UI moderna y reactiva (del template)
- **Hilt** - Inyección de dependencias (del template)
- **Coroutines** - Programación asíncrona
- **ViewModel** - Arquitectura MVVM

### Funcionalidades Karoo
- **DataTypeImpl** - Para crear campos de datos personalizados
- **KarooSystemService** - Interfaz con el sistema Karoo
- **StreamState** - Para recibir datos de sensores en tiempo real
- **RemoteViews** - Para vistas personalizadas (por usar)

### Build Tools
- **Gradle Kotlin DSL** - Build configuration
- **Spotless** - Code formatting
- **GitHub Packages** - Para dependencia karoo-ext

## Estado de Implementación

| Componente | Estado | Notas |
|------------|--------|-------|
| Configuración base | ✅ Completo | Template oficial funcionando |
| Extensión registrada | ✅ Completo | `WPrimeExtension` hereda de `KarooExtension` |
| Data type básico | ✅ Completo | `WPrimeDataType` retransmite potencia |
| Modelo W Prime | ⏳ Por hacer | Necesita implementar cálculos matemáticos |
| Configuración UI | ⏳ Por hacer | Pantalla para CP, W', Tau |
| Almacenamiento | ⏳ Por hacer | DataStore o SharedPreferences |
| Vista personalizada | ⏳ Por hacer | RemoteViews para mostrar W Prime |
| FIT file integration | ⏳ Por hacer | Guardar W Prime en archivos FIT |

## Próximos Pasos

### ✅ Implementación Completada

1. **✅ WPrimeCalculator implementado**:
   ```kotlin
   class WPrimeCalculator(
       private var criticalPower: Double,
       private var anaerobicCapacity: Double,
       private var tauRecovery: Double,
   ) {
       fun updatePower(power: Double, timestamp: Long): Double
       fun getWPrimePercentage(): Double
       fun getTimeToExhaustion(currentPower: Double): Double?
   }
   ```

2. **✅ WPrimeDataType actualizado**:
   - ✅ Integra WPrimeCalculator para cálculos en tiempo real
   - ✅ Carga configuración desde DataStore al inicializar
   - ✅ Proporciona datos W Prime reales a Karoo OS

3. **✅ Interfaz de configuración completa**:
   - ✅ ConfigurationScreen con Compose UI para CP, W', Tau
   - ✅ WPrimeConfigViewModel con gestión de estado
   - ✅ Almacenamiento persistente con Android DataStore

4. **✅ Data type correctamente definido**:
   - ✅ extension_info.xml configurado con `typeId="wprime"`
   - ✅ Descripciones y metadatos actualizados para W Prime

### Desarrollo Pendiente

- **RemoteViews personalizadas** para mostrar W Prime balance gráficamente
- **Integración con FIT files** para guardar datos W Prime en archivos de actividad
- **Alertas en tiempo real** cuando W Prime está bajo (configurables)
- **Validación con datos reales** en dispositivo Karoo
- **Optimización de rendimiento** y ajustes basados en pruebas de usuario

## Diferencias con el Proyecto Anterior

Este proyecto **SÍ usa el framework correcto**:

- ✅ **karoo-ext**: Framework moderno y soportado oficialmente
- ✅ **Proceso separado**: Más estable que el SDK deprecado
- ✅ **Template oficial**: Base sólida del repositorio oficial de Hammerhead
- ❌ **karoo-sdk**: El proyecto anterior usaba el SDK deprecado

## Testing

### En Desarrollo Local
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk
```

### En Karoo Device
1. Habilitar Developer Options en Karoo
2. Conectar via ADB
3. Instalar APK
4. Configurar campos de datos en ride screens

## 🧪 Pruebas y Instalación

### APK Listo para Probar

El APK de desarrollo está disponible en: `app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk`

### Instalación en Karoo

1. **Habilitar instalación desde fuentes desconocidas** en tu Karoo
2. **Transferir el APK** al dispositivo via ADB o tarjeta SD:
   ```bash
   adb install app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk
   ```
3. **Abrir la app** desde el launcher de Karoo
4. **Configurar parámetros W Prime**:
   - Potencia Crítica (CP): Tu FTP × 0.95 (aprox)
   - Capacidad Anaeróbica (W'): 12000-25000 J (típico)
   - Constante de Recuperación (Tau): 200-600 s

### Funcionalidades Disponibles

#### ✅ Configuración
- Interfaz completa para configurar CP, W' y Tau
- Persistencia automática de configuración usando DataStore
- Valores por defecto sensatos para empezar

#### ✅ Cálculo en Tiempo Real
- W Prime se calcula en tiempo real basado en la potencia actual
- Depleción cuando potencia > CP
- Recuperación exponencial cuando potencia < CP
- Datos disponibles como data type para pantallas de Karoo

#### ✅ Data Type para Karoo OS
- Campo de datos "W Prime" disponible en perfiles de riding
- Muestra el valor actual de W Prime en julios
- Se integra nativamente con el sistema de data fields de Karoo

### Próximos Pasos de Prueba

1. **Configurar parámetros** usando la interfaz de la app
2. **Añadir el data field** W Prime a un perfil de riding en Karoo
3. **Realizar un entrenamiento** con variaciones de potencia
4. **Verificar** que W Prime depleta/recupera según esperado
5. **Ajustar parámetros** según experiencia práctica

## 📚 Referencias

### Documentación Oficial
- [Karoo Extensions Documentation](https://hammerheadnav.github.io/karoo-ext/index.html)
- [karoo-ext GitHub Repository](https://github.com/hammerheadnav/karoo-ext)
- [Template Repository](https://github.com/hammerheadnav/karoo-ext-template)

### Community
- [Hammerhead Extensions Developers Forum](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers)

### Conceptos W Prime
- [The Science of Training with Power](https://www.trainingpeaks.com/blog/what-is-w-prime/) - TrainingPeaks
- [Critical Power and W' Research](https://www.cyclinganalytics.com/blog/2018/06/how-does-w-balance-work) - Cycling Analytics

## Debugging y Logging

Este proyecto incluye un sistema de logging unificado y estructurado para facilitar el debugging. Ver [LOGGING.md](LOGGING.md) para detalles completos.

### Comandos útiles para debugging:

```bash
# Ver todos los logs de W Prime
adb logcat | grep "WPrime:"

# Ver solo errores y warnings
adb logcat | grep -E "WPrime:.*(ERROR|WARN)"

# Ver actividad del calculador
adb logcat | grep "WPrime:Calculator"

# Monitorear configuración
adb logcat | grep "WPrime:Settings"
```

### Logging por módulos:
- **Extension**: Ciclo de vida principal
- **DataType**: Streaming de datos en tiempo real
- **Calculator**: Algoritmo de W Prime
- **Settings**: Configuración persistente
- **UI/ViewModel**: Interfaz de usuario

## Licencia

```
Copyright (c) 2025 SRAM LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

**Nota**: Este proyecto está basado en el template oficial de Hammerhead Karoo Extensions y está siendo adaptado para implementar funcionalidad W Prime. El código actual es principalmente del template con modificaciones menores para W Prime.