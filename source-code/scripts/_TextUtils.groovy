/**
 * Utilities for modifying text in files.
 * 
 * @author Francis McKenzie
 * @date 16-April 2012
 */

// -----------------------------------------------------
// -----------------------------------------------------
 
/**
 * Regex search-and-replace on text in a file.
 * 
 * <p> 
 * The simple usage-style is to pass a 'pattern' arg and a 'replacement' arg. The
 * text in the file is then scanned using the 'pattern' regex. If found, all occurrences
 * are replaced with 'replacement'. For example:
 * 
 * <blockquote>
 * def result = findTextInFile(file:file, pattern:/foo/, replacement:"bar")
 * </blockquote>
 * 
 * <p>
 * A more complex usage is to also specify a 'within' arg. This must be a regex pattern
 * that has 3 groups:
 * <p>
 * 1. The entire text in the file BEFORE the part that contains the text to be searched.<br/>
 * 2. The text in the file that needs to have a regex search-and-replace performed on it.<br/>
 * 3. The entire text in the file AFTER the part that contains the text to be searched
 * 
 * <p>
 * For example:
 *
 * <blockquote>
 * def result = findTextInFile(file:file, pattern:/foo/, replacement:"bar", within:/(?ims)\A(.*mysetting=['])([^']*)(['].*)\z/)
 * </blockquote>
 * 
 * <p>
 * Finally, the most sophisticated usage is to specify a 'replacers' arg. This must be a list
 * of Closures that each accept a single string argument. Each Closure should look for text
 * in the string. If found, it should modify the string and return the modified version. If
 * not found, it should throw an Exception. All replacers will be executed in turn on the 
 * text in the file (or the part of the file matched by 'within'), and the final result
 * will be written out to the file. If all of the replacers threw Exceptions (i.e. none
 * of them found a match for the text they were looking for), then the method returns
 * false.
 * <p>
 * A good way to create replacers is to use Groovy's 'curry' function. You can declare
 * a general-purpose replacer like so:
 * <blockquote>
 * def myreplacer = { pattern, replacement, text ->
 *   if (! ((boolean) text =~ pattern) ) {
 *     throw new Exception("NOT FOUND!")
 *   }
 *   return text.replaceAll(pattern, replacement)
 * }
 * </blockquote>
 * <p>
 * You can then create an instance of the replacer for a specific pattern:
 * <blockquote>
 * def result = replaceTextInFile(file:file, replacers:[myreplacer.curry(/foo/, "bar")])
 * </blockquote>
 * 
 * @param file The file containing the text to be changed
 * @param within The pattern to find in the text, which should have 3 groups: BEFORE_TEXT, TEXT_TO_CHANGE, AFTER_TEXT
 * @param pattern The pattern to find in the text
 * @param replacement The replacement for the pattern
 * @param replacers A list of closures that accept a single string arg and return a modified string or throw an Exception if the desired pattern is not found
 * 
 * @return True if at least one pattern was found in the file, regardless of whether any text was actually changed
 */
replaceTextInFile = { args ->
    boolean succeeded = false
    
    // Prepare replacers
    def replacers = args.replacers
    if (! replacers) {
        if (args.pattern && args.replacement) {
            replacers = [patternReplacer.curry(args.pattern, args.replacement)]
        }
    }
    
    // Prepare file
    def file = args.file instanceof File ? args.file : new File("${args.file}")
    
    // Check valid args
    if (!replacers || !file?.exists()) {
        throw new IllegalArgumentException("Invalid args: ${args}")
    }
            
    // Get file contents
    def text = file.text
    
    // Replace text within some block of text
    if (args.within) {
        def m = text =~ args.within
        boolean foundWithin = m
        if (foundWithin && m.groupCount() == 3) {
            def beforeText = m[0][1]
            def oldText = m[0][2]
            def afterText = m[0][3]
            
            try {
                def newText = applyReplacers(oldText, replacers)
                file.withWriter {
                    it << beforeText
                    it << newText
                    it << afterText
                }
                succeeded = true
            } catch (err) { /* No replacers matched */ }
        }
        
    // Just replace text, wherever it is
    } else {
        try {
            def newText = applyReplacers(text, replacers)
            file.text = newText
            succeeded = true
        } catch (err) { /* No replacers matched */ }
    }
    
    return succeeded
}

String applyReplacers(text, replacers) {
    boolean oneMatch = false
    replacers.each {
        try {
            text = it(text)
            oneMatch = true
        } catch (err) { /* No match */ }
    }
    if (!oneMatch) {
        throw new Exception("No matches!")
    }
    return text
}

// This should be curried, e.g.: patternReplacer.curry(/mypattern/, 'myreplacement')
patternReplacer = { pattern, replacement, text ->
    boolean foundPattern = text =~ pattern
    if (! foundPattern) {
        throw new Exception("No match!")
    }
    return text.replaceAll(pattern, replacement)
}
