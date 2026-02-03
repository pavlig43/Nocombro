#!/bin/bash

# web-to-markdown.sh - Convert any webpage to clean Markdown using Jina AI
# Usage: web-to-markdown.sh <url>

if [ -z "$1" ]; then
    echo "❌ Usage: $0 <url>"
    echo ""
    echo "Examples:"
    echo "  $0 https://github.com/VoltAgent/awesome-claude-code-subagents"
    echo "  $0 https://kotlinlang.org/docs/kmp-multiplatform.html"
    echo ""
    echo "Output: Clean Markdown from the webpage"
    exit 1
fi

URL="$1"
echo "📥 Fetching: $URL"
echo "🔄 Converting to Markdown via r.jina.ai..."
echo ""
echo "---"
echo ""

curl -sS "https://r.jina.ai/$URL"

echo ""
echo ""
echo "---"
echo "✅ Done!"
