package org.grails.plugin.vaadin.ui

import com.vaadin.data.Buffered;
import com.vaadin.data.Property;
import com.vaadin.data.Validator.InvalidValueException;

import org.apache.commons.logging.LogFactory;
import org.vaadin.easyuploads.UploadField;

import com.vaadin.terminal.Resource;
import com.vaadin.terminal.StreamResource;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Upload;

/**
 * Overrides Vaadin EasyUpload Addon UploadField to provide following improvements:
 * <ul>
 * <li>Show link to resource that was uploaded, and delete button</li>
 * <li>Show link to value in field (if exists) before any uploads are done</li>
 * <li>Show or hide buttons depending on read-only status of component</li>
 * <li>Ability to handle 'derived' propertyDataSources like PropertyFormatter</li>
 * <li>Fixes NPE bug in UploadField (v1.0.0)</li>
 * <li>Expose upload component to allow listeners to be added</li>
 * </ul>
 * 
 * @author Francis McKenzie
 */
class DefaultUploadField extends UploadField {
    def log = LogFactory.getLog(this.class)
    
    /**
     * Allow us to listen for datasource changes, but not enter an endless
     * loop if we're the ones making the change
     */
    protected boolean committingValueToDataSource
    
    /**
     * The default filename to use when a propertyDataSource is first set for this
     * field, and the propertyDataSource has a value.
     */
    String filename
    /**
     * The default mimeType to use when a propertyDataSource is first set for this
     * field, and the propertyDataSource has a value.
     */
    String mimeType
    /**
     * The default cache time to use when a propertyDataSource is first set for this
     * field, and the propertyDataSource has a value.
     */
    Long cacheTime

    /**
     * The upload file link display
     */
    protected UploadedFileLink uploadedFileLink
    /**
     * The underlying upload component to which listeners can be attached.
     */
    protected Upload uploadComponent
    /**
     * The underlying upload component to which listeners can be attached.
     */
    public Upload getUploadComponent() {
        return this.uploadComponent
    }
    
    /**
     * Construct an upload field with the default storage mode
     */
    public DefaultUploadField() {
        super()
        
        // Bit of a hack, as the upload field in the superclass is private :(
        rootLayout.componentIterator.each {
            if (it instanceof Upload) uploadComponent = it
        }
    }
    
    /**
     * Construct an upload field with the specified storage more
     */
    public DefaultUploadField(UploadField.StorageMode mode) {
        super(mode)
        
        // Bit of a hack, as the upload field in the superclass is private :(
        rootLayout.componentIterator.each {
            if (it instanceof Upload) uploadComponent = it
        }
    }
    
    /**
     * Always update the display when we attach
     */
    public void attach() {
        updateDisplay()
    }
    
    /**
     * Overrides superclass to provide link to file that was uploaded,
     * and delete button
     */
    @Override
    protected void updateDisplay() {
        // Don't do anything if we're not attached
        if (! this.parent) return
        
        // Always remove existing display
        removeUploadedFileLink()
        
        // Add new link if we have a file
        if (! this.empty) { addUploadedFileLink() }
        
        // Always refresh display to reflect read-only status
        updateReadOnlyDisplay()
    }
    
    /**
     * Adds link to uploaded resource and delete button
     */
    protected void addUploadedFileLink() {
        // Create resource filename
        String resourceFileName = (lastFileName ?: filename) ?: "file"
        String resourceMimeType = lastMimeType ?: mimeType
        Long resourceCacheTime = readOnly ? cacheTime : 0 // No caching when uploading
        
        // Create resource
        final DefaultUploadField field = this
        StreamResource resource = new StreamResource(
            new StreamResource.StreamSource() {
                public InputStream getStream() { return field.contentAsStream }
            }, resourceFileName, field.application
        )
        resource.MIMEType = resourceMimeType
        if (cacheTime != null) resource.cacheTime = resourceCacheTime
        
        // Add file link
        uploadedFileLink = new UploadedFileLink(resource, resourceFileName, deleteCaption)
        rootLayout.addComponentAsFirst(uploadedFileLink)
        
        // Set full width
        rootLayout.setWidth(-1)
        rootLayout.setSizeFull()
    }
    
    /**
     * Removes link to uploaded resource and delete button
     */
    protected void removeUploadedFileLink() {
        if (uploadedFileLink) {
            rootLayout.removeComponent(uploadedFileLink)
            uploadedFileLink = null
        }
    }
    
    /**
     * Removes the uploaded file, and clears the uploaded display
     */
    protected void deleteUpload() {
        value = null
        updateDisplay()
    }
    
    /**
     * Catch NullPointerException caused by bug in superclass.
     * <p>
     * Also, set a flag to indicate that valueChange events should
     * be ignored until this method is finished.
     */
    @Override
    public void setValue(Object value) {
        def originalFlag = committingValueToDataSource
        committingValueToDataSource = true
        try {
            super.setValue(value)
        } catch(NullPointerException npe) {
            /* IGNORE */
        } finally {
            committingValueToDataSource = originalFlag
        }
    }
    
    /**
     * Set a flag to indicate that valueChange events should
     * be ignored until this method is finished.
     */
    public void commit() throws Buffered.SourceException, InvalidValueException {
        def originalFlag = committingValueToDataSource
        committingValueToDataSource = true
        try {
            super.commit()
        } finally {
            committingValueToDataSource = originalFlag
        }
    }
    
    /**
     * Unless we're the ones that caused the value to change,
     * we need to update the display to reflect the new value.
     */
    public void valueChange(Property.ValueChangeEvent event) {
        if (!committingValueToDataSource) {
            uploadComponent.receiver.setValue(propertyDataSource.getValue())
            updateDisplay()
        }
    }

    /**
     * Updates the upload field's display to reflect readOnly status
     */
    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly)
        updateReadOnlyDisplay()
    }

   /**
    * Updates the display to reflect the current readOnly status
    */
    protected void updateReadOnlyDisplay() {
        if (uploadedFileLink) uploadedFileLink.deleteButton.visible = !readOnly
        if (uploadComponent) uploadComponent.visible = !readOnly
    }
    
    /**
     * Displays link to uploaded file and delete button
     * 
     * @author Francis McKenzie
     */
    protected class UploadedFileLink extends HorizontalLayout {
        protected Link uploadedLink
        protected Button deleteButton
        
        public UploadedFileLink(StreamResource resource, String fileCaption, String deleteCaption) {
            spacing = true
            
            // File Link
            uploadedLink = new Link(fileCaption, resource)
            addComponent(uploadedLink)
            setComponentAlignment(uploadedLink, Alignment.MIDDLE_LEFT)
            uploadedLink.requestRepaint()
            
            // Delete Button
            deleteButton = new Button(deleteCaption)
            deleteButton.addListener(new Button.ClickListener() {
                public void buttonClick(ClickEvent arg0) { deleteUpload() }
            })
            deleteButton.setStyleName("small")
            addComponent(deleteButton)
            setComponentAlignment(deleteButton, Alignment.MIDDLE_LEFT)
        }
    }
}
