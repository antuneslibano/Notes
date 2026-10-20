# Minhas Notas

Aplicativo Android de bloco de notas simples, 100% offline. Tudo fica salvo apenas no celular.

## Como instalar

1. Abra a página **Releases** deste repositório no celular.
2. Baixe o arquivo `MinhasNotas.apk` da versão mais recente.
3. Abra o arquivo e confirme a instalação (se o Android pedir, permita "Instalar apps desconhecidos" para o navegador).

Versões novas podem ser instaladas por cima da antiga, sem perder as notas.

## Funções

- Editor em blocos no estilo do Notion antigo: digite `/` em qualquer linha para escolher
  `/h1` `/h2` `/h3` `/texto` `/lista` (ou `/list`) `/numerada` `/todo` `/citacao` `/destaque` `/codigo` `/divisor`
- Atalhos ao digitar no começo da linha: `# ` `## ` `### ` `- ` `1. ` `[] ` `> ` ` ``` ` `---`
- Selecione palavras para aplicar negrito, itálico, sublinhado, tachado, cor do texto e cor de fundo
- Listas de tarefas, com marcadores ou numeradas, em qualquer ponto de qualquer nota
- Salvamento automático enquanto você escreve, gravado de forma segura no armazenamento interno
- Notas fixadas, favoritos e pastas
- Pesquisa (ignora acentos), filtro por tipo e ordenação por data de alteração, data de criação ou nome
- Lixeira com recuperação (itens são apagados de vez após 30 dias) e confirmação antes de excluir
- Desfazer e refazer no editor
- Duplicar nota, copiar todo o conteúdo, mudar ícone, mover para pasta, remover tarefas concluídas
- Contador de palavras e caracteres; datas de criação e de alteração
- Modo claro, escuro ou seguindo o sistema
- Bloqueio opcional por PIN e biometria
- Backup manual (.json), restauração (mesclar ou substituir) e exportação como texto (.txt)

Sem inteligência artificial, calendário, contas, nuvem ou colaboração.

## Compilação

O APK é gerado automaticamente pelo GitHub Actions (`.github/workflows/build-apk.yml`) a cada push.
Localmente, com o Android SDK instalado:

```
./gradlew assembleRelease
```

O APK sai em `app/build/outputs/apk/release/app-release.apk`.

O app é assinado com `app/release.keystore`, que está no repositório para que todas as versões
tenham a mesma assinatura e possam ser atualizadas sem desinstalar.
