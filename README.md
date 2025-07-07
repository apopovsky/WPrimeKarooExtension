# W Prime Extension para Hammerhead Karoo

Una extensión para Hammerhead Karoo basada en el nuevo framework **karoo-ext** que implementará el modelo de W Prime (W') para el seguimiento de la energía anaeróbica durante el entrenamiento y las carreras.

## Estado Actual del Proyecto

### ✅ FUNCIONAL Y LISTO PARA PRUEBAS EN DISPOSITIVO

Este proyecto tiene una implementación **COMPLETA Y FUNCIONAL** del modelo W Prime:

- **✅ Base del proyecto**: Template oficial karoo-ext funcional
- **✅ Estructura de extensión**: `WPrimeExtension` heredando de `KarooExtension`
- **✅ Campo de datos W Prime**: `WPrimeDataType` calculando W Prime en tiempo real
- **✅ Configuración persistente**: DataStore integrado para CP, W' y Tau
- **✅ Interfaz de configuración**: UI completa con ConfigurationScreen y ViewModel
- **✅ Cálculo matemático**: WPrimeCalculator implementado con modelo completo y robusto
- **✅ Integración completa**: Configuración persistente vinculada con cálculo en tiempo real
- **✅ Compilación exitosa**: APK generado exitosamente (WPrimeExtension-v1.0-debug.apk)
- **✅ Sistema de logging unificado**: Implementado sistema estructurado para debugging
- **✅ Visualización de zonas de potencia**: Background color coding basado en % de CP
- **✅ Suavizado de datos**: Power smoothing de 5 segundos para estabilidad
- **✅ Simulación para testing**: TestPowerDataSource con patrones realistas de ciclismo
- **✅ Datos para Karoo OS**: W Prime disponible como data field nativo
- **🔄 En progreso**: Pruebas en dispositivo y validación de usuario
- **⏳ Pendiente**: RemoteViews para visualización gráfica personalizada
- **⏳ Pendiente**: Integración con archivos FIT para histórico
- **⏳ Pendiente**: Alertas configurables cuando W Prime está bajo

## ¿Qué es W Prime (W')?

W Prime (W') es un modelo fisiológico que cuantifica la capacidad de trabajo anaeróbico de un ciclista:

- **Potencia Crítica (CP)**: El máximo esfuerzo sostenible teóricamente indefinido
- **W Prime (W')**: La cantidad finita de trabajo que se puede realizar por encima de CP
- **Recuperación**: W' se recupera exponencialmente cuando la potencia está por debajo de CP

### 🧮 Modelo Matemático IMPLEMENTADO

El proyecto incluye un **WPrimeCalculator robusto** que implementa:

1. **Depleción**: Cuando potencia > CP
   ```
   W'(t) = W'(t-1) - (Potencia - CP) × ΔTiempo
   ```

2. **Recuperación**: Cuando potencia < CP (con recuperación adaptativa)
   ```
   W'(t) = W'(t-1) + (W'máx - W'(t-1)) × (1 - e^(-ΔTiempo/τ_efectivo))
   τ_efectivo = τ / (1 + intensidad_recuperación × 0.5)
   ```

3. **Equilibrio**: Cuando potencia = CP
   ```
   W'(t) = W'(t-1) (sin cambio)
   ```

**Características avanzadas implementadas**:
- Validación de entrada robusta (potencia 0-2000W, tiempo delta máximo)
- Recuperación adaptativa basada en intensidad del déficit de potencia
- Logging de cambios significativos y milestones de recuperación
- Funciones auxiliares: tiempo hasta agotamiento, tiempo hasta recuperación completa
- Suavizado de potencia de 5 segundos para estabilidad

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
│   │   ├── MainActivity.kt                 # ✅ Activity principal con TabLayout
│   │   ├── ConfigurationScreen.kt         # ✅ Pantalla de configuración W Prime
│   │   ├── WPrimeApplication.kt           # ✅ Application class con Hilt y Timber
│   │   ├── ViewModelModule.kt             # ✅ Módulo de inyección de dependencias
│   │   ├── ui/
│   │   │   ├── viewmodel/
│   │   │   │   └── WPrimeConfigViewModel.kt  # ✅ ViewModel para configuración
│   │   │   └── components/
│   │   │       └── ConfigurationCard.kt   # ✅ Componente UI para parámetros
│   │   ├── extension/
│   │   │   ├── WPrimeExtension.kt         # ✅ Extensión principal (completa)
│   │   │   ├── WPrimeDataType.kt          # ✅ Campo de datos W Prime (integrado)
│   │   │   ├── WPrimeCalculator.kt        # ✅ Motor de cálculo (robusto y completo)
│   │   │   ├── WPrimeSettings.kt          # ✅ Configuración con DataStore
│   │   │   ├── TestPowerDataSource.kt     # ✅ Simulador de datos para testing
│   │   │   ├── Extensions.kt              # ✅ Funciones auxiliares
│   │   │   └── ServiceModule.kt           # ✅ Módulo de servicios Hilt
│   │   └── utils/                         # ✅ Utilidades del proyecto
│   │       ├── WPrimeLogger.kt           # ✅ Sistema de logging unificado
│   │       └── LogConstants.kt           # ✅ Constantes para logging
│   ├── src/main/res/xml/
│   │   └── extension_info.xml             # ✅ Definición de la extensión
│   └── manifest.json                      # ✅ Metadatos de la app
├── lib/                                   # Librería karoo-ext (código fuente)
├── build.gradle.kts                      # Configuración de build
├── LOGGING.md                            # ✅ Documentación del sistema de logging
└── README.md                             # Este archivo
```

### Archivos Clave Implementados

- **`WPrimeExtension.kt`**: ✅ Clase principal que hereda de `KarooExtension`
- **`WPrimeDataType.kt`**: ✅ Implementa `DataTypeImpl` con cálculo integrado y visualización por zonas
- **`WPrimeCalculator.kt`**: ✅ Algoritmo completo y robusto de W Prime con depleción/recuperación adaptativa
- **`WPrimeSettings.kt`**: ✅ Configuración persistente usando Android DataStore
- **`ConfigurationScreen.kt`**: ✅ UI completa para configurar CP, W' y Tau con validation
- **`WPrimeConfigViewModel.kt`**: ✅ ViewModel que conecta UI con configuración persistente
- **`TestPowerDataSource.kt`**: ✅ Simulador de datos de potencia realistas para testing
- **`WPrimeLogger.kt`**: ✅ Sistema de logging modular y estructurado
- **`WPrimeApplication.kt`**: ✅ Application class con Hilt y Timber initialization
- **`extension_info.xml`**: ✅ Define los data types disponibles para Karoo OS
- **`manifest.json`**: ✅ Metadatos para instalación via Karoo Companion App

## Tecnologías y Dependencias

### Framework Principal
- **Hammerhead karoo-ext 1.1.5** - Framework oficial para extensiones
- **Kotlin** - Lenguaje principal
- **Android API Level 23-35** - Compatibilidad con Karoo devices

### UI y Arquitectura
- **Jetpack Compose** - UI moderna y reactiva
- **Hilt** - Inyección de dependencias para módulos y ViewModels
- **Coroutines + Flow** - Programación asíncrona y streaming de datos
- **ViewModel** - Arquitectura MVVM para UI
- **DataStore** - Almacenamiento persistente de configuración

### Funcionalidades Karoo
- **DataTypeImpl** - Para crear campos de datos personalizados integrados
- **KarooSystemService** - Interfaz con el sistema Karoo para streaming de datos
- **StreamState** - Para recibir datos de sensores en tiempo real
- **ViewEmitter** - Para visualización personalizada con background colors
- **Power Zone Colors** - Coding visual basado en % de Critical Power
- **RemoteViews** - Para vistas personalizadas avanzadas (futuro)

### Build Tools
- **Gradle Kotlin DSL** - Build configuration
- **Spotless** - Code formatting
- **GitHub Packages** - Para dependencia karoo-ext

## Estado de Implementación

| Componente | Estado | Notas |
|------------|--------|-------|
| Configuración base | ✅ Completo | Template oficial funcionando |
| Extensión registrada | ✅ Completo | `WPrimeExtension` hereda de `KarooExtension` |
| Data type W Prime | ✅ Completo | `WPrimeDataType` con cálculo real de W Prime |
| Modelo W Prime | ✅ Completo | `WPrimeCalculator` implementado y robusto |
| Configuración UI | ✅ Completo | ConfigurationScreen con Compose completa |
| Almacenamiento | ✅ Completo | DataStore persistente con validación |
| Logging sistema | ✅ Completo | WPrimeLogger modular y estructurado |
| Visualización zonas | ✅ Completo | Background colors por % de Critical Power |
| Simulación testing | ✅ Completo | TestPowerDataSource con patrones realistas |
| Suavizado de datos | ✅ Completo | Power smoothing 5 segundos |
| Vista personalizada | ⏳ Pendiente | RemoteViews para gauge W Prime |
| FIT file integration | ⏳ Pendiente | Guardar W Prime en archivos FIT |
| Alertas configurables | ⏳ Pendiente | Notificaciones cuando W Prime bajo |

## Próximos Pasos

### ✅ Implementación Completada

**El proyecto está FUNCIONAL y listo para pruebas en dispositivo**

1. **✅ WPrimeCalculator robusto implementado**:
   ```kotlin
   class WPrimeCalculator(
       private var criticalPower: Double,
       private var anaerobicCapacity: Double,
       private var tauRecovery: Double,
   ) {
       fun updatePower(power: Double, timestamp: Long): Double
       fun getWPrimePercentage(): Double
       fun getTimeToExhaustion(currentPower: Double): Double?
       fun getTimeToFullRecovery(currentPower: Double): Double?
       fun reset()
   }
   ```

2. **✅ WPrimeDataType completamente funcional**:
   - ✅ Integra WPrimeCalculator para cálculos en tiempo real
   - ✅ Carga configuración desde DataStore al inicializar
   - ✅ Proporciona datos W Prime reales como porcentaje a Karoo OS
   - ✅ Visualización por zonas de potencia con background colors
   - ✅ Suavizado de potencia de 5 segundos para estabilidad
   - ✅ Modo preview con simulación realista

3. **✅ Interfaz de configuración completa**:
   - ✅ ConfigurationScreen con Compose UI moderna
   - ✅ WPrimeConfigViewModel con gestión de estado
   - ✅ Almacenamiento persistente con Android DataStore
   - ✅ Validación de entrada y valores por defecto sensatos

4. **✅ Sistema completo integrado**:
   - ✅ extension_info.xml configurado con `typeId="wprime"`
   - ✅ Hilt dependency injection para módulos
   - ✅ WPrimeApplication con inicialización Timber
   - ✅ TestPowerDataSource para testing sin sensor real
   - ✅ Sistema de logging modular y estructurado

### Desarrollo Pendiente (Mejoras Opcionales)

- **RemoteViews personalizadas** para mostrar W Prime balance con gauge gráfico
- **Integración con FIT files** para guardar datos W Prime en archivos de actividad
- **Alertas en tiempo real** cuando W Prime está bajo (configurables por usuario)
- **Análisis histórico** de patrones de W Prime en entrenamientos
- **Optimización de rendimiento** basada en testing extensivo en dispositivo
- **Personalización avanzada** de visualización y alertas

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
- Interfaz completa para configurar CP, W' y Tau con validación
- Persistencia automática de configuración usando DataStore
- Valores por defecto sensatos para empezar (CP: 250W, W': 12000J, Tau: 300s)
- ViewModel con gestión de estado reactiva

#### ✅ Cálculo en Tiempo Real
- W Prime se calcula en tiempo real basado en la potencia actual del sensor
- Depleción lineal cuando potencia > CP
- Recuperación exponencial adaptativa cuando potencia < CP
- Suavizado de potencia de 5 segundos para estabilidad
- Validación robusta de entrada y manejo de errores
- Logging detallado de cambios significativos

#### ✅ Data Type para Karoo OS
- Campo de datos "W Prime" disponible en perfiles de riding
- Muestra el porcentaje actual de W Prime (formato PERCENT_MAX_FTP)
- Se integra nativamente con el sistema de data fields de Karoo
- Visualización por zonas de potencia con background colors
- Modo preview con simulación realista para testing

#### ✅ Simulación y Testing
- TestPowerDataSource con patrones realistas de ciclismo
- Warmup, intervals, recovery, sprint patterns programados
- Permite testing completo sin sensor de potencia real
- Datos variados para validar todos los aspectos del algoritmo

### Próximos Pasos de Prueba

1. **Configurar parámetros** usando la interfaz de la app:
   - Critical Power (CP): Tu FTP × 0.95 aproximadamente
   - Anaerobic Capacity (W'): 12000-25000J (típico para ciclistas)
   - Tau Recovery: 200-600s (varía por individuo)

2. **Añadir el data field** W Prime a un perfil de riding en Karoo:
   - Ir a Settings → Ride Profiles → [Tu perfil]
   - Añadir "W Prime" como data field en alguna pantalla

3. **Realizar un entrenamiento** con variaciones de potencia:
   - Observar depleción durante esfuerzos > CP
   - Verificar recuperación durante descansos < CP
   - Notar cambios de color de fondo según zona de potencia

4. **Usar modo simulación** para testing inicial:
   - La app incluye TestPowerDataSource con patrones realistas
   - Warmup, intervals, sprints programados automáticamente
   - Perfecto para validar algoritmo sin sensor real

5. **Ajustar parámetros** según experiencia práctica:
   - Observar comportamiento durante entrenamientos conocidos
   - Comparar con sensación percibida de fatiga anaeróbica
   - Refinar valores CP, W' y Tau según necesidad

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

**Nota**: Este proyecto implementa una **extensión W Prime completamente funcional** basada en el template oficial de Hammerhead Karoo Extensions. El código de W Prime es **original y funcional**, mientras que la base del proyecto utiliza el framework karoo-ext moderno y soportado oficialmente.