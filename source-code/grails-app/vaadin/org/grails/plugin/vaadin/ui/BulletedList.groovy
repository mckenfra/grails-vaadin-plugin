package org.grails.plugin.vaadin.ui

import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;

/**
 * Mimics an HTML bulleted list (i.e. the '&lt;ul&gt;' tag)
 * 
 * @author Francis McKenzie
 */
class BulletedList extends GridLayout {
    /**
     * Provides way of globally changing the default bullet symbol,
     * if no explicit bullet symbol is specified when constructing
     * this bulleted list.
     */
    static String defaultBulletSymbol = "•"
   
    /**
     * Use this to change the bullet symbol that is used when adding
     * new rows 
     */
    String bulletSymbol
    
    /**
     * Construct a bulleted list with default bullet symbol 
     */
    public BulletedList() {
        this(null)
    }
    
    /**
     * Construct a bulleted list with specified bullet symbol 
     */
    public BulletedList(String bulletSymbol) {
        super(2,1)
        this.bulletSymbol = bulletSymbol
        this.spacing = true
        this.columns = 2
    }
    
    /**
     * Adds the specified caption with a bullet next to it.
     */
    @Override
    public void addComponent(Component caption) {
        // Add row
        this.rows = this.rows+1
        
        // Create bullet symbol
        def bulletSymbolText = this.bulletSymbol ?: (defaultBulletSymbol ?: "")
        def bulletSymbolLabel = new Label(bulletSymbolText, Label.CONTENT_RAW)
        bulletSymbolLabel.addStyleName("bullet")
        
        // Add bullet and caption
        setBullet(bulletSymbolLabel, this.rows-1)
        setCaption(caption, this.rows-1)
    }
    
    /**
     * Get the bullet component for the specified row
     */
    public Component getBullet(int row) { getComponent(0, row) }
    /**
     * Set the bullet component for the specified row
     */
    public setBullet(Component bullet, int row) { addComponent(bullet, 0, row) }
    /**
     * Get the caption component for the specified row
     */
    public Component getCaption(int row) { getComponent(1, row) }
    /**
     * Set the caption component for the specified row
     */
    public setCaption(Component caption, int row) { addComponent(caption, 1, row) }
}
