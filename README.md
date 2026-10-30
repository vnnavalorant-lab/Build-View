# Modo Voo (Fabric 1.20.1)

## O que faz
- Bloco "Bloco de Upgrade" (craftável, receita abaixo). Clique direito -> tela com botão **Upgrade**.
- Ao apertar: terceira pessoa com câmera mais perto (e levemente no ombro), voo, atravessar blocos e sem dano, tudo no survival.
- Para sair: tecla **V** (configurável em Controles -> "Modo Voo") ou abrir o bloco de novo (botão "Sair do modo").
  Ao sair dentro de blocos, o jogador sobe até o primeiro espaço livre.

Receita:  F E F / E D E / F E F   (F = pena, E = pérola do fim, D = diamante)

## Compilar
1. Instale o JDK 17.
2. Abra a pasta no IntelliJ IDEA (importa o Gradle sozinho) e rode a task `build`
   OU, no terminal, com Gradle instalado: `gradle wrapper --gradle-version 8.5` e depois `./gradlew build`.
3. O .jar sai em `build/libs/voomod-1.0.0.jar` (use o que NÃO termina em -sources).
4. Coloque na pasta mods junto com o Fabric API 0.92.x para 1.20.1.

## Textura
Troque `src/main/resources/assets/voomod/textures/block/upgrade_block.png` (16x16).

## Ajustar a câmera
`src/main/java/com/example/voomod/mixin/client/CameraMixin.java` -> DISTANCIA, OMBRO, ALTURA.

## Build automático (GitHub Actions)
Suba o projeto para um repositório no GitHub. O arquivo `.github/workflows/build.yml` compila a cada push.
Baixe o .jar na aba **Actions** -> execução -> artefato `voomod` (use o que NÃO termina em -sources).
