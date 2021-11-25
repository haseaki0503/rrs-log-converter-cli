

(function () {

    let DEBUG=false;

    let events = {
        show: 'vcol.event.show',
        shown: 'vcol.event.shown',
        hide:'vcol.event.hide',
        hidden: 'vcol.event.hidden',
        action: 'vcol.event.action'
    };

    window.vcol = {
        event: events
    };

    function vcol_system() {
        // Select target
        let targets = $(this).attr('vcol-toggle').split(/\s+/);
        let source = $(this)[0];

        // Select Toggle Shown
        let beShow = [];
        for (let target of targets) {
            if (!$(target).hasClass('show')) {
                beShow.push(target);
            }
            if (DEBUG) console.log("show: ", beShow);
        }

        // Send Action for Target
        $(targets).trigger(events.action);
        
        // Collect Groups
        let beHide = new Set();
        for (let target of targets) {
            let group = $(target).attr('vcol-group');
            if (group) { // Group Defined
                if (DEBUG) console.log(group);
                $.each($('[vcol-group="' + group + '"]'), function(k, v) {
                    if ($(v).hasClass('show')) {
                        beHide.add(v); // Collect all Members contains target
                    }
                });
            }
            else if ($(target).hasClass('show')) {
                beHide.add(target); // Add Myself
            }
        }

        // Be Hide all
        let hide = Array.from(beHide);
        let hidden = $(hide).hasClass('show'); // Any Item Shown
        if (DEBUG) console.log('hide to :', hide);
        for (let v of hide) {
            $(v).removeClass('show');
            // Send will be hide
            $(v).trigger(events.hide, {source: this, hidden: hide, show: beShow});
        }

        // Set Timer 
        if (DEBUG) console.log(hide);
        if (DEBUG) console.log('shown: ', beShow);
        if (hidden) {
            // After Transition
            $(hide).on('transitionend', {hidden: hide, show: beShow, source: this}, function(evt) {
                $(evt.data.hidden).off('transitionend');

                // Send be hidden
                $(evt.data.hidden).trigger(events.hidden, {source: this, hidden: hide, show: beShow});

                // Show after timeout
                $(evt.data.show).trigger(events.show, evt.data);
                setTimeout(function() {
                    for (let v of evt.data.show) {
                        // Send be shown
                        $(v).trigger(events.shown, evt.data);
                        $(v).addClass('show');
                    }
                }, 500);
            });
        }
        else {
            // Immidiately
            if (DEBUG) console.log('single');
            if (DEBUG) console.log('shown: ', beShow);
            for (let v of beShow) {
                $(v).trigger(events.shown, {show: beShow, source: this});
                $(v).addClass('show');
            }
        }
    }

    $(document).ready(function () {
//         $('[vcol-toggle]').on('click', vcol_system);
        $('[vcol-toggle]').on('click', vcol_system);
    });


}).call(this);
