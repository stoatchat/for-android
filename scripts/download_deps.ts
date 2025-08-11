import { resolve } from "jsr:@std/path"
import { parseArgs } from "jsr:@std/cli/parse-args"

const args = parseArgs(Deno.args, {
    boolean: ["yes", "y", "auto-accept"],
    alias: {
        "y": "yes"
    }
})

const autoAccept = args.yes || args["auto-accept"]

const outputFolderParent = resolve(Deno.cwd(), "app", "src", "main", "assets")

try {
    Deno.statSync(outputFolderParent)
} catch (_) {
    console.error(
        "\x1b[31m" + // red
            "Did you run this script from the correct directory?" +
            "\x1b[0m"
    )
    console.error(
        "Usage: " +
            "\x1b[35m" + // magenta
            "deno run -A scripts/download_deps.ts [--yes|-y|--auto-accept]" +
            "\x1b[0m" +
            " from the " +
            "\x1b[1;31;4m" + // bold red underline
            "root" +
            "\x1b[0m" +
            " directory of the project."
    )
    Deno.exit(1)
}

const outputFolder = resolve(outputFolderParent, "embedded")

// If it exists, delete it
try {
    Deno.removeSync(outputFolder, { recursive: true })
} catch (_) {
    // Ignore, might not exist
}

// Create the output folder
Deno.mkdirSync(outputFolder, { recursive: true })

const deps = [
    {
        file: "katex.min.css",
        url: "https://cdn.jsdelivr.net/npm/katex@0.16.19/dist/katex.min.css",
    },
    {
        file: "katex.min.js",
        url: "https://cdn.jsdelivr.net/npm/katex@0.16.19/dist/katex.min.js",
    },
    {
        file: "fonts/KaTeX_AMS-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_AMS-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_AMS-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_AMS-Regular.woff",
    },
    {
        file: "fonts/KaTeX_AMS-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_AMS-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Caligraphic-Bold.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Caligraphic-Bold.ttf",
    },
    {
        file: "fonts/KaTeX_Caligraphic-Bold.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Caligraphic-Bold.woff",
    },
    {
        file: "fonts/KaTeX_Caligraphic-Bold.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Caligraphic-Bold.woff2",
    },
    {
        file: "fonts/KaTeX_Caligraphic-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Caligraphic-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Caligraphic-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Caligraphic-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Caligraphic-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Caligraphic-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Fraktur-Bold.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Fraktur-Bold.ttf",
    },
    {
        file: "fonts/KaTeX_Fraktur-Bold.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Fraktur-Bold.woff",
    },
    {
        file: "fonts/KaTeX_Fraktur-Bold.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Fraktur-Bold.woff2",
    },
    {
        file: "fonts/KaTeX_Fraktur-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Fraktur-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Fraktur-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Fraktur-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Fraktur-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Fraktur-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Main-Bold.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Bold.ttf",
    },
    {
        file: "fonts/KaTeX_Main-Bold.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Bold.woff",
    },
    {
        file: "fonts/KaTeX_Main-Bold.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Bold.woff2",
    },
    {
        file: "fonts/KaTeX_Main-BoldItalic.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-BoldItalic.ttf",
    },
    {
        file: "fonts/KaTeX_Main-BoldItalic.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-BoldItalic.woff",
    },
    {
        file: "fonts/KaTeX_Main-BoldItalic.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-BoldItalic.woff2",
    },
    {
        file: "fonts/KaTeX_Main-Italic.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Italic.ttf",
    },
    {
        file: "fonts/KaTeX_Main-Italic.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Italic.woff",
    },
    {
        file: "fonts/KaTeX_Main-Italic.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Italic.woff2",
    },
    {
        file: "fonts/KaTeX_Main-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Main-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Main-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Main-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Math-BoldItalic.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Math-BoldItalic.ttf",
    },
    {
        file: "fonts/KaTeX_Math-BoldItalic.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Math-BoldItalic.woff",
    },
    {
        file: "fonts/KaTeX_Math-BoldItalic.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Math-BoldItalic.woff2",
    },
    {
        file: "fonts/KaTeX_Math-Italic.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Math-Italic.ttf",
    },
    {
        file: "fonts/KaTeX_Math-Italic.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Math-Italic.woff",
    },
    {
        file: "fonts/KaTeX_Math-Italic.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Math-Italic.woff2",
    },
    {
        file: "fonts/KaTeX_SansSerif-Bold.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Bold.ttf",
    },
    {
        file: "fonts/KaTeX_SansSerif-Bold.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Bold.woff",
    },
    {
        file: "fonts/KaTeX_SansSerif-Bold.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Bold.woff2",
    },
    {
        file: "fonts/KaTeX_SansSerif-Italic.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Italic.ttf",
    },
    {
        file: "fonts/KaTeX_SansSerif-Italic.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Italic.woff",
    },
    {
        file: "fonts/KaTeX_SansSerif-Italic.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Italic.woff2",
    },
    {
        file: "fonts/KaTeX_SansSerif-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_SansSerif-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Regular.woff",
    },
    {
        file: "fonts/KaTeX_SansSerif-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_SansSerif-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Script-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Script-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Script-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Script-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Script-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Script-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Size1-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size1-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Size1-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size1-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Size1-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size1-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Size2-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size2-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Size2-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size2-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Size2-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size2-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Size3-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size3-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Size3-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size3-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Size3-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size3-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Size4-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size4-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Size4-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size4-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Size4-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Size4-Regular.woff2",
    },
    {
        file: "fonts/KaTeX_Typewriter-Regular.ttf",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Typewriter-Regular.ttf",
    },
    {
        file: "fonts/KaTeX_Typewriter-Regular.woff",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Typewriter-Regular.woff",
    },
    {
        file: "fonts/KaTeX_Typewriter-Regular.woff2",
        url: "https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/fonts/KaTeX_Typewriter-Regular.woff2",
    },
    {
        file: "micromark.bundle.js",
        url: "https://esm.sh/v135/micromark@3.2.0/es2022/micromark.bundle.mjs",
    },
    {
        file: "micromark-gfm.bundle.js",
        url: "https://esm.sh/v135/micromark-extension-gfm@3.0.0/es2022/micromark-extension-gfm.bundle.mjs",
    }
]

for (const dep of deps) {
    console.log(`- ${dep.file} from ${dep.url}`)
}

if (!autoAccept) {
    console.log("Will download the above files.")
    if (!confirm("Continue?")) {
        console.log("Aborted.")
        Deno.exit(0)
    }
} else {
    console.log("Will download the above files. (auto-accepted)")
}

const fontsFolder = resolve(outputFolder, "fonts")
Deno.mkdirSync(fontsFolder, { recursive: true })

for (const dep of deps) {
    const response = await fetch(dep.url)
    const data = await response.arrayBuffer()
    const file = resolve(outputFolder, dep.file)
    Deno.writeFileSync(file, new Uint8Array(data))
    console.log(`Downloaded ${dep.file} to ${file}`)
}

console.log("Done.")
