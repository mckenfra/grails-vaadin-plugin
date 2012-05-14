import java.beans.java_awt_BorderLayout_PersistenceDelegate;

/**
 * =====================================================
 * 
 * Utilities for modifying Groovy config files
 * 
 * @author Francis McKenzie
 * @date 13-April 2012
 * 
 * =====================================================
 */

includeTargets << new File("${vaadinPluginDir}/scripts/_TextUtils.groovy")

/**
 * For replacing text in Groovy Config.
 * 
 * @param file The config file to update
 * @param within The config text that wraps the block that needs to be updated - see examples below.
 * The text should contain one of the following: *CLOSURE, CLOSURE*, *LIST, LIST*
 *   @arg CLOSURE indicates that the block is a closure-type, i.e. wrapped in curly braces
 *   @arg LIST indicates that the block is a list-type, i.e. wrapped in square brackets, with comma-separated items
 *   @arg '*' indicates where the existing config should be placed. So *CLOSURE will append the
 *        new config, leaving the existing config at the start of the block.
 * 
 * @param configs A list of configs to be added, each config specified as a map with the following properties:
 *   @arg code The config code to insert, unless it already exists.
 *   @arg comment The comment text to insert
 *   @arg test A regular expression to test if the config already exists. Can be null, in which case the code itself used as the search criteria
 * 
 * 
 * EXAMPLE - Within Closure
 * 
 * <blockquote><code>
 * 
 * addGroovyConfig(
 *   file:file,
 *   within:'grails.project.dependency.resolution = { repositories *CLOSURE }',
 *   configs:[
 *     [code: 'mavenRepo "http://maven.vaadin.com/vaadin-addons"', comment: 'Vaadin Addons', test:'http://maven.vaadin.com/vaadin-addons']
 *   ]
 * )
 * 
 * </blockquote></code>
 * 
 * EXAMPLE - Within List
 * 
 * <blockquote><code>
 * 
 * addGroovyConfig(
 *   file:file,
 *   within:'class UrlMappings { excludes = *LIST }',
 *   configs:[
 *     [code: '"VAADIN"', comment: 'Vaadin Static Files']
 *   ]
 * )
 * 
 * </blockquote></code>
 */
addGroovyConfig = { args->
    // Check args
    def file = args?.file
    if (!file) {
        throw new IllegalArgumentException("No file specified!")
    }
    def within = toWithinConfigPattern(args.within)
    def isList = isWithinList(args.within)
    def isPrepend = isPrepend(args.within)
    def replacers = []
    args.configs?.each {
        it.separator = isList ? "," : ""
        it.prepend = isPrepend
        replacers << configReplacer.curry(it)
    }
    return replaceTextInFile(file:file, within:within, replacers:replacers)
}

/**
 * Internal patterns
 */
withinClosurePrepend = /(?ims)(?<=^|\s|\{)CLOSURE\*(?>=$|\s|\})/
withinClosureAppend = /(?ims)(?<=^|\s|\{)\*CLOSURE(?>=$|\s|\})/
withinListPrepend = /(?ims)(?<=^|\s|\{)LIST\*(?>=$|\s|\})/
withinListAppend = /(?ims)(?<=^|\s|\{)\*LIST(?>=$|\s|\})/

/**
 * If true, then the replacement is within a list.
 */
protected boolean isWithinList(String within) {
    return within =~ withinListPrepend || within =~ withinListAppend
}

/**
 * If true, then should the value be missing, it should be added to the start of the matched block.
 */
protected boolean isPrepend(String within) {
    return within =~ withinClosurePrepend || within =~ withinListPrepend
}

/**
 * Takes the specified pseudo-pattern and converts it into a regex
 */
protected toWithinConfigPattern(within) {
    String result = within?.toString() ?: ""
    // RULE: .
    result = result.replaceAll(/\./, "\\\\.")
    // RULE: *CLOSURE or CLOSURE*
    result = result.replaceAll(withinClosurePrepend, "\\\\{)(?<=^|\\\\s|\\\\{)([^\\\\}]*?)(\\\\s*\\\\}.*")
    result = result.replaceAll(withinClosureAppend, "\\\\{)(?<=^|\\\\s|\\\\{)([^\\\\}]*?)(\\\\s*\\\\}.*")
    // RULE: *LIST or LIST*
    result = result.replaceAll(withinListPrepend, "\\\\[)(?<=^|\\\\s|\\\\[)([^\\\\]]*?)(\\\\s*\\\\].*")
    result = result.replaceAll(withinListAppend, "\\\\[)(?<=^|\\\\s|\\\\[)([^\\\\]]*?)(\\\\s*\\\\].*")
    // RULE: {
    result = result.replaceAll(/(?<!\\)\{\s*/, "\\\\{.*(?<=^|\\\\s|\\\\{)")
    // RULE: }
    result = result.replaceAll(/(?<!\\)\}\s*/, "\\\\}.*")
    // RULE: ' '
    result = result.replaceAll(/ /, "\\\\s*")
    // RULE: ^...$
    result = result.replaceAll(/(?ims)\A(.*)\z/, "(?ims)\\\\A(.*(?<=^|\\\\s)\$1.*)\\\\z")
        
    return result
}

/**
 * For replacing items in config, for example <code>excludes = ["a", "b", "c"]</code>
 * 
 * As a replacer, it should be curried, i.e.:
 *   configReplacer.curry('"VAADIN"', 'Vaadin Static files', null, ",", true)
 */
configReplacer = { Map args, text ->
    // Check args
    def code = args.code
    def comment = args.comment
    def separator = args.separator ?: ""
    
    // Check actually have some items already, and not just comments or whitespace
    def alreadyHasItemsRegex = /(?m)^\s*[\"\'\w\[\{]/
    boolean alreadyHasItems = text =~ alreadyHasItemsRegex
    
    // Check if already exists
    def test = args.tester ?: (args.code?.toString() ?: /.*/)
    def tests = [test]
    if (test instanceof String) {
        if (test.startsWith('"') && test.endsWith('"') && test.length() > 2) {
            tests << "'" + test[1..test.length()-2] + "'"
        } else if (test.startsWith("'") && test.endsWith("'") && test.length() > 2) {
            tests << '"' + test[1..test.length()-2] + '"'
        } else {
            tests << "'${test}'"
            tests << "\"${test}\""
        }
    }
    tests.each {
        if ( (it instanceof java.util.regex.Pattern && text =~ it) ||
           (!(it instanceof java.util.regex.Pattern) && text.contains(it)) ) {
           throw new Exception("Already contains!")
        }
    }
    
    // PREPEND
    def result
    if (args.prepend) {
        result = """\

        // ${comment}
        ${code}${alreadyHasItems ? separator : ''}
${text}"""

    // APPEND
    } else {
        result = """\
${text}${alreadyHasItems ? separator : ''}

        // ${comment}
        ${code}"""
    }
    
    return result
}