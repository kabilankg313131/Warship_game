package com.example.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.model.ShipType

/**
 * ============================================================================
 * SHIP DOCK VIEW: Visual Fleet Selection Panel
 * ============================================================================
 * Displays the 5 distinct warships with their real sizes and visual silhouettes:
 *  - Top Row: Patrol Boat (Size 2), Submarine (Size 3), Cruiser (Size 3)
 *  - Bottom Row: Battleship (Size 4), Aircraft Carrier (Size 5)
 *
 * Supports tapping to select a ship for grid placement, and visually dims
 * already-placed ships (ghosted outline) matching the reference UI.
 */
class ShipDockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val shipViews = mutableMapOf<ShipType, ImageView>()
    private var onShipSelectedListener: ((ShipType) -> Unit)? = null

    private var activeSelectedType: ShipType = ShipType.CARRIER
    private var placedShipTypes = setOf<ShipType>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun setOnShipSelectedListener(listener: (ShipType) -> Unit) {
        this.onShipSelectedListener = listener
    }

    fun setupDock(fleet: List<ShipType>) {
        removeAllViews()
        shipViews.clear()

        // Row 1: Ships of length 2 and 3 (Patrol Boat, Submarine, Cruiser)
        val row1 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(8)
            }
        }

        // Row 2: Heavy capital ships of length 4 and 5 (Battleship, Aircraft Carrier)
        val row2 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        for (ship in fleet) {
            val img = ImageView(context).apply {
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    activeSelectedType = ship
                    onShipSelectedListener?.invoke(ship)
                    refreshDock()
                }
            }
            shipViews[ship] = img

            val weight = ship.length.toFloat()
            val lp = LayoutParams(0, dpToPx(44), weight).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
            img.layoutParams = lp

            if (ship.length <= 3) {
                row1.addView(img)
            } else {
                row2.addView(img)
            }
        }

        addView(row1)
        addView(row2)

        post {
            refreshDock()
        }
    }

    private var sunkShipTypes = setOf<ShipType>()

    fun updateState(placedTypes: Set<ShipType>, selectedType: ShipType) {
        this.placedShipTypes = placedTypes
        this.activeSelectedType = selectedType
        refreshDock()
    }

    fun updateFleetCombatState(sunkTypes: Set<ShipType>) {
        this.sunkShipTypes = sunkTypes
        refreshDock()
    }

    fun refreshDock() {
        for ((type, img) in shipViews) {
            val w = img.width.coerceAtLeast(dpToPx(type.length * 20))
            val h = img.height.coerceAtLeast(dpToPx(38))
            val isPlaced = placedShipTypes.contains(type)
            val isSelected = (type == activeSelectedType)
            val isSunk = sunkShipTypes.contains(type)

            val bmp = WarshipRenderer.createDockShipBitmap(
                shipType = type,
                widthPx = w,
                heightPx = h,
                isPlaced = isPlaced,
                isSelected = isSelected,
                isSunk = isSunk
            )
            img.setImageBitmap(bmp)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
