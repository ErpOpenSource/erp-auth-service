# Pull Request Guidelines — erp-auth-service

Estas pautas aseguran calidad, trazabilidad y consistencia.

## Objetivo de un PR

Un PR debe:
- resolver un problema o entregar una funcionalidad concreta,
- ser revisable en < 15 minutos,
- tener impacto acotado,
- incluir pruebas o una justificación clara si no aplica.

## Tamaño recomendado

- Ideal: 50–300 líneas netas.
- Evitar PRs gigantes que mezclen refactor + feature + infra.
- Si es grande, dividir por commits o por PRs secuenciales.

## Requisitos mínimos

Todo PR debe incluir:

1. **Descripción clara** del cambio.
2. **Motivación** (por qué se hace).
3. **Cómo probar** (pasos o evidencia).
4. **Impacto** (breaking changes, DB changes, compatibilidad).
5. **Checklist** completo.

## Arquitectura (reglas de oro)

- No meter lógica de negocio en controllers.
- Validaciones de dominio en `domain/`.
- Casos de uso en `application/`.
- Adaptadores (DB, JWT, hashing) en `infrastructure/`.
- Errores estandarizados con códigos (ej. `SEAT_LIMIT_REACHED`).

## Database / Bytebase

Si el PR incluye cambios en BDD:

- Añadir script SQL versionado en `db/migrations/XXX_*.sql`.
- No ejecutar migraciones en runtime (no Flyway/Liquibase en la app).
- Documentar el cambio en `docs/data-model.md` si afecta el modelo.
- Indicar en el PR cómo se aplicará con Bytebase.

## Observabilidad

Cualquier endpoint nuevo debe:
- Propagar `X-Request-Id` (recibido o generado).
- Loguear en JSON con requestId y traceId si aplica.
- Exponer métricas si es relevante.

## Checklist de revisión (para reviewers)

- [ ] Scope claro y acotado
- [ ] Convención de commits respetada
- [ ] No hay secretos (tokens, passwords, .env) commiteados
- [ ] Cambios DB versionados y documentados
- [ ] Errores API estandarizados
- [ ] Pruebas añadidas o justificadas