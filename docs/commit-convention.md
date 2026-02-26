# Commit Convention — erp-auth-service

Este repositorio usa **Conventional Commits** para mantener historial limpio,
permitir changelog automático y facilitar CI/CD.

## Formato

<type>(<scope>): <description>

Ejemplo:
feat(auth): enforce license seat limit
fix(session): prevent concurrent login race
docs(readme): add local run instructions

## Types

- feat: nueva funcionalidad
- fix: corrección de bug
- refactor: refactor sin cambio funcional
- perf: mejora de rendimiento
- test: pruebas
- docs: documentación
- chore: tareas internas (deps, tooling)
- build: build system / gradle/maven
- ci: pipelines, GitHub Actions
- revert: revert de un commit

## Scope recomendado

- auth
- user
- session
- license
- rbac
- audit
- infra
- api
- db
- obs (observability)

## Reglas

- Descripciones cortas, en imperativo, sin punto final.
- Un commit por cambio lógico coherente.
- Evitar commits tipo “wip”, “temp”, “fix stuff”.
- Si hay breaking change, añadir `!`:
  - `feat(api)!: change refresh response format`

## Ejemplos válidos

- feat(license): block logins when seat limit reached
- fix(auth): return USER_LOCKED on locked status
- refactor(domain): extract PasswordPolicyService
- docs(db): describe session active rule
- chore(deps): bump spring boot version

## Ejemplos inválidos

- update
- fixed
- changes
- wip commit