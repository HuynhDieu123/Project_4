package com.mypack.admin;

import com.mypack.entity.PointSettings;
import com.mypack.sessionbean.PointSettingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named("adminPointSettingsBean")
@ViewScoped
public class AdminPointSettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PointSettingsFacadeLocal pointSettingsFacade;

    // Only one rule is used, but keep list for the table
    private List<PointSettings> settings;
    private PointSettings selectedSetting;     // current rule (can be null if none yet)

    // form fields
    private Long    editAmountPerPoint;        // how many USD
    private Integer editPointsPerAmount;       // how many points

    // preview
    private Long sampleAmount;                 // sample money
    private long previewPoints;                // calculated points

    @PostConstruct
    public void init() {
        settings = pointSettingsFacade.findAll();
        if (settings == null) {
            settings = new ArrayList<>();
        }

        if (!settings.isEmpty()) {
            // if there are multiple records, only the first one is used
            selectedSetting     = settings.get(0);
            editAmountPerPoint  = selectedSetting.getAmountPerPoint();
            editPointsPerAmount = selectedSetting.getPointsPerAmount();
        } else {
            // no rule yet -> form in "create new" mode
            selectedSetting     = null;
            editAmountPerPoint  = 100L; // default suggestion: 100 USD
            editPointsPerAmount = 1;
        }

        sampleAmount = 500L; // sample: 500 USD
        recalcPreview();
    }

    /**
     * Click Edit in table -> load into form
     */
    public void prepareEdit(PointSettings ps) {
        if (ps == null) {
            return;
        }
        selectedSetting     = ps;
        editAmountPerPoint  = ps.getAmountPerPoint();
        editPointsPerAmount = ps.getPointsPerAmount();
        recalcPreview();
    }

    /**
     * Delete current rule. After delete, the form returns to "create new" mode.
     */
    public void delete(PointSettings ps) {
        if (ps == null) return;

        try {
            PointSettings managed = pointSettingsFacade.find(ps.getSettingId());
            if (managed != null) {
                pointSettingsFacade.remove(managed);
            }

            // refresh list + state
            settings = pointSettingsFacade.findAll();
            if (settings == null) {
                settings = new ArrayList<>();
            }

            selectedSetting     = null;
            editAmountPerPoint  = 100L;
            editPointsPerAmount = 1;
            sampleAmount        = 500L;
            recalcPreview();

            FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Deleted",
                    "The point rule has been deleted. You can create a new rule by entering values and clicking Save.")
            );
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Delete failed",
                    "Unable to delete this point rule.")
            );
        }
    }

    /**
     * Recalculate previewPoints for the current sampleAmount & rule.
     */
    public void recalcPreview() {
        if (sampleAmount == null
                || editAmountPerPoint == null || editAmountPerPoint <= 0
                || editPointsPerAmount == null || editPointsPerAmount <= 0) {
            previewPoints = 0L;
            return;
        }

        long packs = sampleAmount / editAmountPerPoint; // how many "packs" of money
        previewPoints = packs * editPointsPerAmount;    // each pack gives X points
    }

    /**
     * Save rule:
     *  - if no rule yet -> create
     *  - otherwise -> update existing rule
     */
    public void save() {
        if (editAmountPerPoint == null || editAmountPerPoint <= 0
                || editPointsPerAmount == null || editPointsPerAmount <= 0) {

            FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Invalid values",
                    "Amount and points must be greater than 0.")
            );
            return;
        }

        Date now = new Date();

        if (selectedSetting == null || selectedSetting.getSettingId() == null) {
            // no rule yet -> create new
            PointSettings ps = new PointSettings();
            ps.setAmountPerPoint(editAmountPerPoint);
            ps.setPointsPerAmount(editPointsPerAmount);
            ps.setCreatedAt(now);
            ps.setUpdatedAt(null);
            pointSettingsFacade.create(ps);
            selectedSetting = ps;
        } else {
            // update current rule
            selectedSetting.setAmountPerPoint(editAmountPerPoint);
            selectedSetting.setPointsPerAmount(editPointsPerAmount);
            selectedSetting.setUpdatedAt(now);
            pointSettingsFacade.edit(selectedSetting);
        }

        // reload list from DB (even though only one record is used)
        settings = pointSettingsFacade.findAll();
        if (settings == null) {
            settings = new ArrayList<>();
        }

        FacesContext.getCurrentInstance().addMessage(
            null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Saved",
                "Point rule has been updated successfully.")
        );

        recalcPreview();
    }

    // ===== GET / SET =====

    public List<PointSettings> getSettings() {
        return settings;
    }

    public PointSettings getSelectedSetting() {
        return selectedSetting;
    }

    public void setSelectedSetting(PointSettings selectedSetting) {
        this.selectedSetting = selectedSetting;
    }

    public Long getEditAmountPerPoint() {
        return editAmountPerPoint;
    }

    public void setEditAmountPerPoint(Long editAmountPerPoint) {
        this.editAmountPerPoint = editAmountPerPoint;
    }

    public Integer getEditPointsPerAmount() {
        return editPointsPerAmount;
    }

    public void setEditPointsPerAmount(Integer editPointsPerAmount) {
        this.editPointsPerAmount = editPointsPerAmount;
    }

    public Long getSampleAmount() {
        return sampleAmount;
    }

    public void setSampleAmount(Long sampleAmount) {
        this.sampleAmount = sampleAmount;
        recalcPreview();
    }

    public long getPreviewPoints() {
        return previewPoints;
    }
}
