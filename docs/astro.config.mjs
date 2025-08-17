import { defineConfig } from "astro/config"
import starlight from "@astrojs/starlight"
import markdownIntegration from "@astropub/md"

// https://astro.build/config
export default defineConfig({
    site: "https://peptidechat.github.io",
    base: "/android",
    integrations: [
        starlight({
            title: "Peptide on Android Technical Documentation",
            social: {
                github: "https://github.com/peptidechat/android",
            },
            sidebar: [
                {
                    label: "Contributing",
                    items: [
                        // Each item here is one entry in the navigation menu.
                        {
                            label: "Guidelines",
                            link: "/contributing/guidelines",
                        },
                        {
                            label: "Setup",
                            link: "/contributing/setup",
                        }
                    ],
                },
                {
                    label: "Beta Test",
                    items: [
                        {
                            label: "Geographic Availability",
                            link: "/beta/availability-regions",
                        },
                        {
                            label: "Join the Beta Test",
                            link: "/beta/joining-the-public-beta-test",
                        }
                    ],
                },
                /* {
                    label: "Reference",
                    autogenerate: { directory: "reference" },
                }, */
            ],
            customCss: ["./src/styles/custom.css"],
        }),
        markdownIntegration(),
    ],
})
