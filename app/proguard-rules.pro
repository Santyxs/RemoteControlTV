# Compose y AndroidX ya traen sus propias reglas de consumidor (consumer-rules.pro)
# incluidas en sus AARs, así que no hace falta duplicarlas aquí.

# Mantener nombres de nuestras clases selladas/enums: los usamos en mensajes de
# error (javaClass.simpleName) y no queremos que R8 los renombre a "a", "b", etc.
-keepnames class pwf.xenova.tvremote.SendResult
-keepnames class pwf.xenova.tvremote.SendResult$*
-keepnames class pwf.xenova.tvremote.RemoteAction
-keepnames class pwf.xenova.tvremote.TvBrand

# Evitar warnings ruidosos por referencias opcionales de coroutines/compose
-dontwarn kotlinx.coroutines.**
