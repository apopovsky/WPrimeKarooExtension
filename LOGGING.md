# Sistema de Logging para W Prime Extension

## Resumen

Se ha implementado un sistema de logging unificado y estructurado para mejorar la capacidad de debugging y monitoreo del W Prime Extension. El sistema reemplaza los logs dispersos con mensajes consistentes, informativos y categorizados.

## Arquitectura del Sistema de Logging

### WPrimeLogger (Clase Principal)

Ubicación: `app/src/main/kotlin/com/itl/wprimeext/utils/WPrimeLogger.kt`

Funciones principales:
- **Prefijo uniforme**: Todos los logs llevan el prefijo "WPrime:[Módulo]"
- **Niveles estándar**: Debug (d), Info (i), Warning (w), Error (e)
- **Métodos especializados** para escenarios comunes de debugging

#### Módulos Definidos:
- `EXTENSION`: Ciclo de vida principal de la extensión
- `DATA_TYPE`: Streaming y procesamiento de datos en tiempo real
- `CALCULATOR`: Cálculos del algoritmo W Prime
- `SETTINGS`: Persistencia y configuración
- `UI`: Interfaz de usuario
- `VIEWMODEL`: Gestión de estado en ViewModels

### LogConstants (Constantes de Mensajes)

Ubicación: `app/src/main/kotlin/com/itl/wprimeext/utils/LogConstants.kt`

Contiene constantes predefinidas para mensajes comunes:
- Ciclo de vida de la extensión
- Operaciones de streaming de datos
- Cálculos de W Prime
- Operaciones de configuración
- Eventos de UI
- Operaciones de archivos FIT

## Ejemplos de Uso

### Logging Básico
```kotlin
// Antes
Timber.d("Doughnuts now $doughnuts")

// Después
WPrimeLogger.logDataFlow(WPrimeLogger.Module.EXTENSION, "FIT record write", "doughnuts: $doughnuts")
```

### Logging de Configuración
```kotlin
WPrimeLogger.logConfiguration(
    WPrimeLogger.Module.CALCULATOR,
    criticalPower,
    anaerobicCapacity,
    tauRecovery
)
```

### Logging de Errores
```kotlin
WPrimeLogger.logError(WPrimeLogger.Module.SETTINGS, "configuration load", exception)
```

## Salida de Ejemplo

Los logs ahora aparecen con el siguiente formato:
```
WPrime:Extension - Extension started - Version 1.0
WPrime:Settings - Configuration - CP: 250.0W, W': 12000.0J, Tau: 300.0s
WPrime:Calculator - Power: 280.0W -> W': 11500J (96%)
WPrime:DataType - Data stream started
```

## Beneficios del Nuevo Sistema

### 1. **Identificación Clara**
- Cada log indica claramente qué aplicación (`WPrime`) y módulo lo generó
- Fácil filtrado por categorías en logcat

### 2. **Información Útil para Debugging**
- Los valores de potencia incluyen W' actual y porcentaje restante
- Los cambios de configuración muestran todos los parámetros
- Los eventos del ciclo de vida son claramente marcados

### 3. **Consistencia**
- Formato uniforme en toda la aplicación
- Mensajes predefinidos evitan inconsistencias
- Niveles de log apropiados (Debug para detalles, Info para eventos importantes)

### 4. **Rendimiento**
- Logging inteligente: solo cambios significativos o actualizaciones periódicas
- Evita spam en logs con datos que cambian constantemente

## Archivos Modificados

1. **WPrimeExtension.kt**
   - Reemplazado logging de "doughnuts" con contexto de FIT file operations
   - Añadido logging del ciclo de vida de la extensión
   - Mejorado logging de eventos de broadcast

2. **WPrimeDataType.kt**
   - Añadido logging de inicio/parada de streams
   - Contextualizado el logging de generación de datos de prueba

3. **WPrimeCalculator.kt**
   - Logging detallado de actualizaciones de configuración
   - Alertas cuando W' se agota completamente
   - Logging inteligente de cambios de potencia (solo cambios significativos)

4. **WPrimeSettings.kt**
   - Logging de operaciones de carga/guardado
   - Diferenciación entre configuración por defecto y personalizada

5. **WPrimeConfigViewModel.kt**
   - Logging de cambios de UI y flujo de datos

## Uso para Debugging

### Filtros útiles en Android Studio/logcat:
```bash
# Ver solo logs de W Prime
adb logcat | grep "WPrime:"

# Ver solo un módulo específico
adb logcat | grep "WPrime:Calculator"

# Ver solo eventos importantes
adb logcat | grep -E "WPrime:.*(started|stopped|error|failed)"
```

### Debugging de problemas comunes:

**1. Configuración no se carga:**
```
WPrime:Settings - Using default settings
WPrime:Settings - Configuration - CP: 250.0W, W': 12000.0J, Tau: 300.0s
```

**2. W Prime no actualiza:**
```
WPrime:Calculator - Configuration - CP: 250.0W, W': 12000.0J, Tau: 300.0s
WPrime:Calculator - Power: 280.0W -> W': 11200J (93%)
```

**3. Problemas de conexión:**
```
WPrime:Extension - Failed to connect to Karoo service
```

## Próximos Pasos

1. **Logging Condicional**: Implementar niveles de logging configurables (Debug/Release)
2. **Métricas de Rendimiento**: Añadir logging de tiempo de ejecución para operaciones críticas
3. **Logging de FIT Files**: Añadir detalles sobre escritura exitosa/fallida de archivos FIT
4. **Crash Reporting**: Integrar el sistema con herramientas de crash reporting

## Notas de Mantenimiento

- Usar siempre `WPrimeLogger` en lugar de `Timber` directamente
- Añadir nuevos módulos a `WPrimeLogger.Module` si es necesario
- Añadir constantes a `LogConstants` para mensajes que se repiten
- Mantener el balance entre información útil y rendimiento
