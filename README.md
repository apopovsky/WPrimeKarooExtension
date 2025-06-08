# W Prime Extension para Hammerhead Karoo

Una extensión completa para Hammerhead Karoo que implementa el modelo de W Prime (W') para el seguimiento de la energía anaeróbica durante el entrenamiento y las carreras.

## Características

### ✅ Completado

- **Campo de datos de Potencia**: Muestra la potencia instantánea en watts
- **Campo de datos W Prime**: Calcula y muestra el balance de energía anaeróbica en tiempo real
- **Pantalla de configuración completa** con parámetros personalizables:
  - Potencia Crítica (CP) - watts
  - Capacidad Anaeróbica (W') - julios (default: 12000J)
  - Constante de Recuperación (Tau) - segundos (default: 300s)
- **Almacenamiento persistente** de configuración usando DataStore
- **Cálculos precisos** del modelo W Prime con depleción y recuperación
- **Interfaz moderna** con Material Design 3
- **Compilación exitosa** y APK generado (16.3 MB)

### 🧮 Modelo W Prime Implementado

El modelo matemático incluye:

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

## Instalación

1. Compila el proyecto:
   ```bash
   ./gradlew assembleDebug
   ```

2. Instala el APK en tu Hammerhead Karoo:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. En tu Karoo, agrega los campos de datos disponibles:
   - **Potencia (W)**: Para ver la potencia instantánea
   - **W Prime (J)**: Para ver el balance de energía anaeróbica

## Configuración

1. Abre la aplicación W Prime Extension en tu Karoo
2. Configura tus parámetros personalizados:
   - **Potencia Crítica**: Tu CP en watts (típicamente de un test de 20 min × 0.95)
   - **Capacidad Anaeróbica**: Tu W' en julios (típicamente 10000-25000J)
   - **Tau**: Constante de recuperación en segundos (típicamente 200-600s)

## Uso en Entrenamiento

1. Agrega el campo "W Prime (J)" a tu pantalla de datos
2. El valor se actualiza en tiempo real basado en tu potencia
3. Monitorea tu reserva de energía anaeróbica durante intervalos intensos
4. Utiliza los períodos de recuperación para restaurar tu W Prime

## Estructura del Proyecto

```
app/
├── src/main/kotlin/com/itl/wprimeextension/
│   ├── WPrimeExtension.kt          # Extensión principal
│   ├── data/
│   │   └── WPrimeSettings.kt       # Configuración con DataStore
│   ├── wprime/
│   │   └── WPrimeCalculator.kt     # Motor de cálculo W Prime
│   ├── ui/
│   │   ├── components/
│   │   │   └── ConfigurationCard.kt
│   │   └── viewmodel/
│   │       └── WPrimeConfigViewModel.kt
│   └── screens/
│       ├── MainScreen.kt           # Pantalla principal
│       ├── ConfigurationScreen.kt  # Configuración
│       └── WPrimeStatusCard.kt     # Estado actual
```

## Tecnologías Utilizadas

- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI moderna y reactiva
- **DataStore Preferences** - Almacenamiento de configuración
- **Coroutines** - Programación asíncrona
- **Material Design 3** - Sistema de diseño moderno
- **Hammerhead Karoo SDK** - Integración con el dispositivo

### Dependencias Esenciales
- `karoo-ext` - SDK de Hammerhead Karoo
- `compose-bom` - Bill of Materials para Compose
- `datastore-preferences` - Persistencia de configuración
- `lifecycle-viewmodel-compose` - ViewModel para Compose
- `kotlinx-coroutines` - Programación asíncrona
- `spotless` - Formateo automático de código

## Estado de Desarrollo

| Característica | Estado |
|---------------|--------|
| Campo de datos Potencia | ✅ Completo |
| Campo de datos W Prime | ✅ Completo |
| Configuración UI | ✅ Completo |
| Cálculo W Prime | ✅ Completo |
| Almacenamiento config | ✅ Completo |
| Compilación exitosa | ✅ Completo |
| APK generado | ✅ Completo |
| Código optimizado | ✅ Completo |

## Optimizaciones Realizadas

### Dependencias Limpiadas
- **Reducción de ~25 a ~12 dependencias**: Eliminadas librerías no utilizadas
- **Eliminación de frameworks innecesarios**: Timber, Glance, Navigation, Mapbox, Hilt, etc.
- **Solo dependencias esenciales**: Karoo SDK, Compose, DataStore, Coroutines

### Archivos Eliminados
- ❌ `datafield/WPrimeDataField.kt` - Archivo no utilizado
- ❌ `drawable/ic_wprime.xml` - Recurso no utilizado
- ❌ `strings.xml/hello_karoo` - String no utilizado

### Plugins Optimizados
- **Reducción de 8 a 3 plugins**: Solo Android Application, Kotlin Android, Kotlin Compose
- **Eliminados**: Hilt, KSP, Serialization, Dokka, Android Library

### Build Configuration
- **Sin ViewBinding**: No se usa en la app
- **Sin configuraciones de test**: Tests no implementados
- **Configuración minimalista**: Solo lo esencial para funcionar

## Próximos Pasos (Opcional)

- Pruebas en dispositivo real con medidor de potencia
- Optimización de rendimiento para cálculos en tiempo real
- Validación de precisión del modelo con datos reales
- Implementación de alertas cuando W Prime esté bajo

## Links

[Documentación Karoo SDK](https://hammerheadnav.github.io/karoo-ext/index.html)

[karoo-ext source](https://github.com/hammerheadnav/karoo-ext)
