# Branching Strategy — erp-auth-service

Este repositorio sigue un flujo de ramas pensado para entornos enterprise con despliegue controlado.

## Objetivos

- Mantener `main` siempre en estado **deployable** (listo para producción).
- Usar `develop` como rama de **integración**.
- Trabajar con ramas cortas y revisiones obligatorias vía Pull Request.
- Facilitar hotfixes urgentes sin romper el flujo.

## Ramas

### Ramas principales

- `main` → Producción (código estable, versionable, deployable).
- `develop` → Integración (código listo para QA/integración entre features).

### Ramas de trabajo

- `feature/*` → Nuevas funcionalidades.
- `hotfix/*` → Correcciones urgentes en producción.

## Flujo estándar (Feature)

1. Crear rama desde `develop`:
   - `feature/<breve-descripcion>`
2. Abrir Pull Request hacia `develop`.
3. Merge a `develop` tras pasar checks y aprobación.
4. Cuando el release esté listo:
   - PR desde `develop` hacia `main`.

## Flujo urgente (Hotfix)

1. Crear rama desde `main`:
   - `hotfix/<breve-descripcion>`
2. PR hacia `main` y merge tras validación.
3. Backport obligatorio:
   - PR desde `hotfix/*` hacia `develop` (o cherry-pick si aplica).

## Convenciones de nombres

- `feature/login-seat-enforcement`
- `feature/refresh-rotation`
- `hotfix/fix-seat-race-condition`

Evitar:
- `feature/test`
- `feature/aaa`
- `fix1`, `branch2`

## Reglas obligatorias (GitHub Branch Protection)

Se recomienda configurar en GitHub:

### Para `main`
- Require Pull Request before merging
- Require at least 1 approval
- Require status checks to pass
- Require linear history
- Include administrators

### Para `develop`
- Require Pull Request before merging
- Require at least 1 approval
- Require status checks to pass (cuando existan)